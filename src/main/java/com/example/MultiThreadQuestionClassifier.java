package com.example;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service   //define business logic
public class MultiThreadQuestionClassifier {
    private final QuestionRepository questionRepository;

    public MultiThreadQuestionClassifier(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public Map<MultiThreadingCategory, List<QuestionDTO>> classifyAllQuestions() {
        List<QuestionItem> allQuestions = questionRepository.findAll();
        return allQuestions.stream()
                .filter(question -> MultiThreadingCategory.classify(question.getBody()) != null)   //都是多线程相关的类
                .map(question -> {
                    MultiThreadingCategory category = MultiThreadingCategory.classify(question.getBody());   //每个问题分类
                    return new QuestionDTO(
                            question.getQuestion_id(),
                            question.getTitle(),
                            question.getBody(),
                            category
                    );
                })
                .collect(Collectors.groupingBy(QuestionDTO::getCategory));
    }

    public Map<String, Long> countCategoryDistribution() {
        Map<MultiThreadingCategory, List<QuestionDTO>> classifiedMap = classifyAllQuestions();
        // 便于画图
        return classifiedMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> (long) entry.getValue().size()
                ));
    }

    public static class QuestionDTO {
        private int questionId;
        private String title;
        private String body;
        private MultiThreadingCategory category;

        public QuestionDTO(int questionId, String title, String body, MultiThreadingCategory category) {
            this.questionId = questionId;
            this.title = title;
            this.body = body;
            this.category = category;
        }

        public MultiThreadingCategory getCategory() {
            return category;
        }
    }
}