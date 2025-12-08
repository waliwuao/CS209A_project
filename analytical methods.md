# Analytical methods

## Tasks 1
用sql语句，按照时间跨度的具体情况给时间戳分组，建立多个视图，在每一组里面统计每个tag出现的频次，然后把同一个标签在不同时间段的频次综合到一个表中，寻找变化趋势。

#### sql
```sql

```

## Tasks 2
用sql语句，关联question_tag和tag表，将tag_id映射为tag_name，得到question_tag_name视图。然后在视图中通过连接得到一对标签两两出现的频数，最后通过公式$Support(tag_A, tag_B) = \frac{A, B共同出现次数}{总问题数}$和$Lift(A, B) = \frac{Support(tag_A, tag_B)}{Support(tag_A)·Support(tag_B)}$得到标签两两之间的关联性。

#### sql
```sql
-- 去重并创建临时表（避免修改原表），一行里面只有一个question_id和一个tag_name
CREATE TABLE question_tag_clean AS
SELECT DISTINCT question_id, tag_id FROM question_tag;

-- 关联 tag 表，得到 question_id + tag_name 的映射
CREATE VIEW question_tag_name AS
SELECT q.question_id, t.tag_name 
FROM question_tag_clean q
JOIN tag t ON q.tag_id = t.tag_id;

-- 问题总数
SELECT COUNT(DISTINCT question_id) AS total_questions FROM question_tag_clean;

-- 单个 tag 的支持度（Support(A) = 单个 tag 出现次数 / 总问题数）
CREATE VIEW tag_support AS
SELECT 
    tag_name,
    COUNT(DISTINCT question_id) AS total_count,
    COUNT(DISTINCT question_id) / (SELECT total_questions FROM dual) AS support
FROM question_tag_name
GROUP BY tag_name;

-- 计算 tag 两两的 Lift 值
SELECT 
    t1.tag_name AS tag_a,
    t2.tag_name AS tag_b,
    COUNT(DISTINCT t1.question_id) AS co_occur_count,
    -- 支持度 Support(A,B)
    COUNT(DISTINCT t1.question_id) / (SELECT total_questions FROM dual) AS support_ab,
    -- 置信度 Confidence(A→B)
    COUNT(DISTINCT t1.question_id) / ta.total_count AS confidence_a2b,
    -- 提升度 Lift(A,B)
    (COUNT(DISTINCT t1.question_id) / (SELECT total_questions FROM dual)) / (ta.support * tb.support) AS lift
FROM question_tag_name t1
JOIN question_tag_name t2 
    ON t1.question_id = t2.question_id 
    AND t1.tag_name < t2.tag_name
JOIN tag_support ta ON t1.tag_name = ta.tag_name
JOIN tag_support tb ON t2.tag_name = tb.tag_name
GROUP BY t1.tag_name, t2.tag_name
HAVING lift > 1  -- 只保留正相关的 tag 组合
ORDER BY lift DESC;
```

## Task 3
在`java/com/example`文件夹中，`MultiTreadingCategory.java`包含关于多线程的标签，每个标签中有相关的关键词及其正则表达式，有需要时可以修改，也包含给单个问题分配标签所用到的方法， `MultiThreadQuestionClassifier.java`中包含批量处理问题用到的方法。批量处理问题是通过`QuestionRepository.java`实现的，通过仓库的`findAll()`方法得到所有问题，然后用处理单个问题的方法得到问题对应的标签，并用标签将问题分组。