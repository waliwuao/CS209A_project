package com.example;

import java.util.List;
import java.util.regex.Pattern;

// 主题枚举（定义所有要分析的主题）
enum TargetTopic {
    MULTITHREADING(
        // 关键词列表
        List.of("deadlock", "race condition", "synchronize", "volatile", "ExecutorService", "ThreadPool"),
        // 正则规则（编译为Pattern提升性能）
        List.of(
            Pattern.compile("java\\.lang\\.IllegalMonitorStateException"),
            Pattern.compile("synchronized\\s*\\("),
            Pattern.compile("new\\s+Thread\\(")
        )
    ),
    SPRING_BOOT_SECURITY(
        List.of("spring-boot", "security", "authentication", "JWT", "OAuth2", "csrf"),
        List.of(
            Pattern.compile("@EnableWebSecurity"),
            Pattern.compile("JwtTokenProvider"),
            Pattern.compile("authentication failed")
        )
    );

    private final List<String> keywords;
    private final List<Pattern> regexPatterns;
    TargetTopic(List<String> keywords, List<Pattern> regexPatterns) {
        this.keywords = keywords;
        this.regexPatterns = regexPatterns;
    }

    // 核心方法：判断文本是否属于当前主题
    public boolean match(String text) {
        if (text == null || text.isEmpty()) return false;
        String lowerText = text.toLowerCase();   // 转小写，忽略大小写

        // 1. 关键词匹配（只要包含1个关键词，就初步命中）
        boolean keywordMatch = keywords.stream()
                .anyMatch(keyword -> lowerText.contains(keyword.toLowerCase()));

        // 2. 正则匹配（进一步验证，提高精准度）
        boolean regexMatch = regexPatterns.stream()
                .anyMatch(pattern -> pattern.matcher(lowerText).find());

        // 关键词 + 正则 满足一个即可（可根据需求调整为“同时满足”）
        return keywordMatch || regexMatch;
    }

    public List<String> getKeywords() {
        return keywords;
    }
    public List<Pattern> getRegexPatterns() {
        return regexPatterns;
    }
}

public class TopicMatcherUtil {   // 工具类：对外提供主题匹配服务
    // 判断文本是否属于目标主题
    public static boolean isRelatedToTopic(String text, TargetTopic topic) {
        return topic.match(text);
    }

    // 进阶：统计文本中命中主题的关键词/正则次数（用于评分，判断相关性强弱）
    public static int countTopicMatches(String text, TargetTopic topic) {
        if (text == null || text.isEmpty()) return 0;
        String lowerText = text.toLowerCase();
        int count = 0;

        // 统计关键词命中次数
        count += topic.getKeywords().stream()
                .filter(keyword -> lowerText.contains(keyword.toLowerCase()))
                .count();

        // 统计正则命中次数（每个正则匹配1次算1分）
        count += topic.getRegexPatterns().stream()
                .filter(pattern -> pattern.matcher(lowerText).find())
                .count();

        return count;
    }
}