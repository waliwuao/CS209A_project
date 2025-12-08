package com.example;

import org.jsoup.Jsoup;
import java.util.List;
import java.util.regex.Pattern;

// 多线程细分分类枚举
public enum MultiThreadingCategory {
    DEADLOCK(
        List.of("deadlock", "dead locked", "lock order", "circular wait"),
        List.of(
            Pattern.compile("deadlock\\s*(detected|occurred|error)"),
            Pattern.compile("circular wait"),
            Pattern.compile("java\\.lang\\.Deadlock")
        )
    ),
    RACE_CONDITION(
        List.of("race condition", "data race", "concurrent modify", "shared variable", "atomicity"),
        List.of(
            Pattern.compile("race condition"),
            Pattern.compile("non-atomic"),
            Pattern.compile("shared variable.*not synchronized")
        )
    ),
    THREAD_POOL(
        List.of("thread pool", "ThreadPoolExecutor", "ExecutorService", "corePoolSize", "rejected execution"),
        List.of(
            Pattern.compile("ThreadPoolExecutor\\("),
            Pattern.compile("ExecutorService\\.(newFixedThreadPool|newCachedThreadPool)"),
            Pattern.compile("RejectedExecutionException")
        )
    ),
    SYNCHRONIZATION(
        List.of("synchronized", "ReentrantLock", "Lock", "Condition", "wait\\(\\)", "notify\\(\\)"),
        List.of(
            Pattern.compile("synchronized\\s*\\("),
            Pattern.compile("ReentrantLock"),
            Pattern.compile("wait\\(\\)|notify\\(\\)|notifyAll\\(\\)")
        )
    ),
    VOLATILE(
        List.of("volatile", "visibility", "ordering", "JMM", "happens-before", "double-checked locking"),
        List.of(
            Pattern.compile("volatile\\s+\\w+"),
            Pattern.compile("double-checked locking"),
            Pattern.compile("happens-before")
        )
    ),
    CONCURRENT_COLLECTION(
        List.of("ConcurrentHashMap", "CopyOnWriteArrayList", "ConcurrentLinkedQueue", "thread-safe collection"),
        List.of(
            Pattern.compile("ConcurrentHashMap"),
            Pattern.compile("CopyOnWrite\\w+"),
            Pattern.compile("thread-safe.*collection")
        )
    ),
    OTHER(
        List.of("ThreadLocal", "interrupt", "daemon thread", "join\\(\\)", "sleep\\(\\)", "yield\\(\\)"),
        List.of(
            Pattern.compile("ThreadLocal"),
            Pattern.compile("thread\\.interrupt\\(\\)"),
            Pattern.compile("daemon thread")
        )
    );

    // 核心属性：关键词列表、正则Pattern列表
    private final List<String> coreKeywords;
    private final List<Pattern> regexPatterns;
    MultiThreadingCategory(List<String> coreKeywords, List<Pattern> regexPatterns) {
        this.coreKeywords = coreKeywords;
        this.regexPatterns = regexPatterns;
    }

    private String cleanText(String rawText) {
        if (rawText == null || rawText.isEmpty()) return "";
        // 1. 去除HTML标签（Stack Overflow的body含<code>、<p>等）
        String plainText = Jsoup.parse(rawText).text();
        // 2. 转小写，方便后续匹配
        return plainText.toLowerCase().trim();
    }

    public int calculateScore(String rawText) {
        String text = cleanText(rawText);
        if (text.isEmpty()) return 0;

        int score = 0;

        // 1. 关键词匹配：遍历所有核心关键词，命中1个+1分
        for (String keyword : coreKeywords) {
            if (text.contains(keyword.toLowerCase())) {
                score += 1;
            }
        }
        // 2. 正则匹配：遍历所有正则，命中1个+2分（正则更精准，权重更高）
        for (Pattern pattern : regexPatterns) {
            if (pattern.matcher(text).find()) {
                score += 2;
            }
        }

        return score;
    }

    public static MultiThreadingCategory classify(String rawText) {
        // 1. 初始化最高得分和对应分类
        int maxScore = 0;
        MultiThreadingCategory result = null;

        // 2. 遍历所有分类，计算得分
        for (MultiThreadingCategory category : values()) {
            int currentScore = category.calculateScore(rawText);
            // 3. 更新最高得分（若得分相同，按枚举定义的优先级取靠前的分类）
            if (currentScore > maxScore) {
                maxScore = currentScore;
                result = category;
            }
        }

        return maxScore > 0 ? result : null;
    }
}