# Analytical methods

## Tasks 1



## Tasks 2

用sql语句，关联question_tag和tag表，将tag_id映射为tag_name，得到question_tag_name视图。然后在视图中通过连接得到一对标签两两出现的频数，最后通过公式$Support(tag_A, tag_B) = \frac{A, B共同出现次数}{总问题数}$和$Lift(A, B) = \frac{Support(tag_A, tag_B)}{Support(tag_A)·Support(tag_B)}$得到标签两两之间的关联性。

#### sql
```sql
    -- 去重并创建临时表（避免修改原表）
    CREATE TABLE question_tag_clean AS
    SELECT DISTINCT question_id, tag_id FROM question_tag;

    -- 关联 tag 表，得到 question_id + tag_name 的映射
    CREATE VIEW question_tag_name AS
    SELECT q.question_id, t.tag_name 
    FROM question_tag_clean q
    JOIN tag t ON q.tag_id = t.tag_id;

    -- 统计 tag 两两共现的次数（A在前、B在后，避免重复计算）
    SELECT 
        t1.tag_name AS tag_a,
        t2.tag_name AS tag_b,
        COUNT(DISTINCT t1.question_id) AS co_occur_count  -- 共现的问题数
    FROM question_tag_name t1
    JOIN question_tag_name t2 
        ON t1.question_id = t2.question_id  -- 同一问题
        AND t1.tag_name < t2.tag_name       -- 避免 (A,B) 和 (B,A) 重复
    GROUP BY t1.tag_name, t2.tag_name
    ORDER BY co_occur_count DESC;  -- 按共现次数降序排列

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