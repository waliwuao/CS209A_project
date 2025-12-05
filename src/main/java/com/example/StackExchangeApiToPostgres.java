package com.example;

import com.google.gson.Gson;
import java.io.FileWriter;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.time.temporal.TemporalAdjusters;

import static java.lang.Thread.sleep;

public class StackExchangeApiToPostgres {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/cs209a_project";
    private static final String USER = "postgres";
    private static final String PASSWORD = "Dr141592";
    private static final String API_URL = "https://api.stackexchange.com/2.3/search";
    private static final String SITE = "stackoverflow.com";
    private static final int PAGE_SIZE = 100;
    private static final String FILTER = "withbody";
    private static final String TAGGED = "java";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            conn.setAutoCommit(true);
            Gson gson = new Gson();
            HttpClient client = HttpClient.newHttpClient();

            List<Map<String, Object>> monthDateRanges = generateMonthDateRanges();

            for (Map<String, Object> dateRange : monthDateRanges) {
                long fromDate = (Long) dateRange.get("fromDate");
                long toDate = (Long) dateRange.get("toDate");
                String monthLabel = (String) dateRange.get("label");
                System.out.println("Start processing month: " + monthLabel);

                for (String order : Arrays.asList("asc", "desc")) {
                    System.out.println("  Processing order: " + order);
                    try {
                        String url = buildRequestUrl(order, fromDate, toDate);

                        String jsonResponse = sendGetRequest(client, url);

                        StackOverflowData data = gson.fromJson(new StringReader(jsonResponse), StackOverflowData.class);

                        if (data.getItems() == null || data.getItems().isEmpty()) {
                            System.out.println("  No data retrieved");
                            continue;
                        }

                        importAccounts(conn, data.getItems());
                        importQuestions(conn, data.getItems());
                        Map<String, Integer> tagMap = importTags(conn, data.getItems());
                        importQuestionTags(conn, data.getItems(), tagMap);

                        System.out.println("  Data import completed, total " + data.getItems().size() + " records");
                        sleep(10000);
                    } catch (Exception e) {
                        System.err.println("  Processing failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("All month data processing finished");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Map<String, Object>> generateMonthDateRanges() {
        List<Map<String, Object>> ranges = new ArrayList<>();

        LocalDateTime startMonth = LocalDateTime.of(2025, 4, 1, 0, 0, 0);
        LocalDateTime endMonth = LocalDateTime.of(2025, 4, 1, 0, 0, 0);

        LocalDateTime current = startMonth;
        while (!current.isAfter(endMonth)) {
            LocalDateTime firstDay = current.with(TemporalAdjusters.firstDayOfMonth());
            LocalDateTime lastDay = current.with(TemporalAdjusters.lastDayOfMonth())
                    .withHour(23).withMinute(59).withSecond(59);

            long fromDate = firstDay.toEpochSecond(ZoneOffset.UTC);
            long toDate = lastDay.toEpochSecond(ZoneOffset.UTC);

            Map<String, Object> range = new HashMap<>();
            range.put("fromDate", fromDate);
            range.put("toDate", toDate);
            range.put("label", current.getYear() + "-" + (current.getMonthValue() < 10 ? "0" : "") + current.getMonthValue());
            ranges.add(range);

            current = current.plusMonths(1);
        }

        return ranges;
    }

    private static String buildRequestUrl(String order, long fromDate, long toDate) {
        return API_URL + "?" +
                "order=" + order +
                "&sort=votes" +
                "&tagged=" + TAGGED +
                "&fromdate=" + fromDate +
                "&todate=" + toDate +
                "&site=" + SITE +
                "&pagesize=" + PAGE_SIZE +
                "&filter=" + FILTER;
    }

    private static String sendGetRequest(HttpClient client, String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(request);
        if (response.statusCode() != 200) {
            throw new RuntimeException("API request failed, status code: " + response.statusCode() + ", response: " + response.body());
        }

        return response.body();
    }

    private static void importAccounts(Connection conn, List<QuestionItem> items) throws SQLException {
        Set<Integer> importedAccountIds = new HashSet<>();
        String sql = "INSERT INTO account (account_id, reputation) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (QuestionItem item : items) {
                Owner owner = item.getOwner();
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

    private static void importQuestions(Connection conn, List<QuestionItem> items) throws SQLException {
        String sql = "INSERT INTO question (question_id, account_id, accept_answer_id, view_count, answer_count, " +
                "score, creation_date, title, body) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (QuestionItem item : items) {
                ps.setInt(1, item.getQuestion_id());
                ps.setInt(2, item.getOwner().getAccount_id());
                ps.setObject(3, item.getAccepted_answer_id());
                ps.setInt(4, item.getView_count());
                ps.setInt(5, item.getAnswer_count());
                ps.setInt(6, item.getScore());
                ps.setInt(7, item.getCreation_date());
                ps.setString(8, item.getTitle());
                ps.setString(9, item.getBody());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static Map<String, Integer> importTags(Connection conn, List<QuestionItem> items) throws SQLException {
        Map<String, Integer> tagNameToId = new HashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT tag_id, tag_name FROM tag")) {
            while (rs.next()) {
                tagNameToId.put(rs.getString("tag_name"), rs.getInt("tag_id"));
            }
        }

        String sql = "INSERT INTO tag (tag_name) VALUES (?) RETURNING tag_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (QuestionItem item : items) {
                for (String tagName : item.getTags()) {
                    if (tagNameToId.containsKey(tagName)) continue;

                    ps.setString(1, tagName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            tagNameToId.put(tagName, rs.getInt(1));
                        }
                    }
                }
            }
        }
        return tagNameToId;
    }

    private static void importQuestionTags(Connection conn, List<QuestionItem> items, Map<String, Integer> tagMap) throws SQLException {
        String sql = "INSERT INTO question_tag (question_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (QuestionItem item : items) {
                int questionId = item.getQuestion_id();
                for (String tagName : item.getTags()) {
                    Integer tagId = tagMap.get(tagName);
                    if (tagId == null) continue;

                    ps.setInt(1, questionId);
                    ps.setInt(2, tagId);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }
}