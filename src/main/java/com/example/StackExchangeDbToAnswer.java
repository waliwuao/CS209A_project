package com.example;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class StackExchangeDbToAnswer {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/cs209a_project";
    private static final String USER = "postgres";
    private static final String PASSWORD = "Dr141592";
    private static final String BASE_URL = "https://api.stackexchange.com/2.3";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson gson = new Gson();

    private static final int MAX_BATCH_QUESTION_COUNT = 100;
    private static final int MAX_BATCH_ANSWER_COUNT = 100;
    private static final int MAX_RETRY_COUNT = 3;
    private static final int DELAY_SECONDS = 10;

    public static void main(String[] args) {
        Map<Integer, Integer> questionIdToAnswerCount = getQuestionIdToAnswerCountFromDb();
        System.out.println("Retrieved " + questionIdToAnswerCount.size() + " question IDs with answer counts from database");

        if (questionIdToAnswerCount.isEmpty()) {
            System.out.println("No questions to process");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);

            List<Integer> questionBatch = new ArrayList<>();
            int currentTotalAnswers = 0;

            for (Map.Entry<Integer, Integer> entry : questionIdToAnswerCount.entrySet()) {
                int questionId = entry.getKey();
                int answerCount = entry.getValue();
                int actualFetchCount = Math.min(answerCount, 100);


                boolean reachQuestionLimit = (questionBatch.size() + 1) > MAX_BATCH_QUESTION_COUNT;
                boolean reachAnswerLimit = (currentTotalAnswers + actualFetchCount) > MAX_BATCH_ANSWER_COUNT;

                if (reachQuestionLimit || reachAnswerLimit) {
                    processBatchQuestions(conn, questionBatch, questionIdToAnswerCount);
                    questionBatch.clear();
                    currentTotalAnswers = 0;
                }

                questionBatch.add(questionId);
                currentTotalAnswers += actualFetchCount;
            }

            // 处理剩余的问题批次
            if (!questionBatch.isEmpty()) {
                processBatchQuestions(conn, questionBatch, questionIdToAnswerCount);
            }

            conn.commit();
            System.out.println("Answer data import completed!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 批量处理一组问题，一次请求获取多个问题的答案
     */
    private static void processBatchQuestions(Connection conn, List<Integer> questionBatch,
                                              Map<Integer, Integer> questionIdToAnswerCount) throws Exception {
        System.out.println("Processing " + questionBatch.size() + " questions in batch");

        List<AnswerItem> answers = fetchBatchQuestionsAnswersWithRetry(questionBatch, questionIdToAnswerCount);

        if (answers == null || answers.isEmpty()) {
            System.out.println("No valid answers retrieved for this batch after retries");
            return;
        }

        System.out.println("Fetched " + answers.size() + " answers for batch");
        importAnswerAccounts(conn, answers);
        importAnswers(conn, answers);
        conn.commit();
        System.out.println("Successfully committed batch to database");
    }

    /**
     * 带重试机制的批量获取答案方法
     */
    private static List<AnswerItem> fetchBatchQuestionsAnswersWithRetry(List<Integer> questionIds,
                                                                        Map<Integer, Integer> questionIdToAnswerCount) throws Exception {
        int retryCount = 0;
        List<AnswerItem> answers = null;

        while (retryCount < MAX_RETRY_COUNT) {
            answers = fetchBatchQuestionsAnswers(questionIds, questionIdToAnswerCount);

            boolean hasNullAnswer = answers.stream().anyMatch(Objects::isNull);

            if (answers != null && !answers.isEmpty() && !hasNullAnswer) {
                return answers;
            }

            retryCount++;
            if (retryCount < MAX_RETRY_COUNT) {
                System.out.println("Answers contain null or empty, retrying in " + DELAY_SECONDS + " seconds... (Retry " + retryCount + "/" + MAX_RETRY_COUNT + ")");
                Thread.sleep(DELAY_SECONDS * 1000L);
            }
        }

        System.out.println("Reached maximum retry count (" + MAX_RETRY_COUNT + "), no valid answers retrieved");
        return answers;
    }

    /**
     * 从数据库获取所有需要处理的问题ID及其答案数量
     */
    private static Map<Integer, Integer> getQuestionIdToAnswerCountFromDb() {
        Map<Integer, Integer> questionMap = new HashMap<>();
        String sql = "SELECT question_id, answer_count FROM question WHERE answer_count > 0";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int questionId = rs.getInt("question_id");
                int answerCount = rs.getInt("answer_count");
                questionMap.put(questionId, answerCount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return questionMap;
    }

    /**
     * 批量获取多个问题的答案，使用Stack API的ids参数
     */
    private static List<AnswerItem> fetchBatchQuestionsAnswers(List<Integer> questionIds,
                                                               Map<Integer, Integer> questionIdToAnswerCount) throws Exception {
        // 构建多个问题ID的逗号分隔字符串
        String questionIdsStr = questionIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(";"));

        int maxAnswersInBatch = questionIds.stream()
                .mapToInt(id -> Math.min(questionIdToAnswerCount.getOrDefault(id, 0), 100))
                .max()
                .orElse(100);

        String url = String.format(
                "%s/questions/%s/answers?order=desc&sort=votes&site=stackoverflow&pagesize=100&filter=withbody",
                BASE_URL, questionIdsStr
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        System.out.println("Requesting: " + url);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        AnswerResponse answerResponse = gson.fromJson(response.body(), AnswerResponse.class);

        System.out.println("Waiting " + DELAY_SECONDS + " seconds before next request...");
        Thread.sleep(DELAY_SECONDS * 1000L);

        return answerResponse != null ? answerResponse.getItems() : Collections.emptyList();
    }

    /**
     * 导入答案对应的用户账号信息到数据库
     */
    private static void importAnswerAccounts(Connection conn, List<AnswerItem> answers) throws SQLException {
        Set<Integer> importedAccountIds = new HashSet<>();
        String sql = "INSERT INTO account (account_id, reputation) VALUES (?, ?) ON CONFLICT DO NOTHING";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (AnswerItem answer : answers) {
                Owner owner = answer.getOwner();
                if (owner == null || owner.getAccount_id() == 0) continue;

                int accountId = owner.getAccount_id();
                if (importedAccountIds.contains(accountId)) continue;

                ps.setInt(1, accountId);
                ps.setInt(2, owner.getReputation());
                ps.addBatch();
                importedAccountIds.add(accountId);
            }
            ps.executeBatch();
        }
    }

    /**
     * 导入答案信息到数据库
     */
    private static void importAnswers(Connection conn, List<AnswerItem> answers) throws SQLException {
        String sql = "INSERT INTO answer (answer_id, question_id, account_id, score, creation_date, body) " +
                "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (AnswerItem answer : answers) {
                Owner owner = answer.getOwner();
                if (owner == null || owner.getAccount_id() == 0) continue;

                ps.setInt(1, answer.getAnswer_id());
                ps.setInt(2, answer.getQuestion_id());
                ps.setInt(3, owner.getAccount_id());
                ps.setInt(4, answer.getScore());
                ps.setInt(5, answer.getCreation_date());
                ps.setString(6, answer.getBody());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * 用于解析API响应的实体类
     */
    static class AnswerResponse {
        private List<AnswerItem> items;

        public List<AnswerItem> getItems() {
            return items;
        }
    }

    /**
     * 答案信息实体类
     */
    static class AnswerItem {
        private int answer_id;
        private int question_id;
        private Owner owner;
        private int score;
        private int creation_date;
        private String body;

        public int getAnswer_id() { return answer_id; }
        public int getQuestion_id() { return question_id; }
        public Owner getOwner() { return owner; }
        public int getScore() { return score; }
        public int getCreation_date() { return creation_date; }
        public String getBody() { return body; }
    }

    /**
     * 答案作者信息实体类
     */
    static class Owner {
        private int account_id;
        private int reputation;

        public int getAccount_id() { return account_id; }
        public int getReputation() { return reputation; }
    }
}