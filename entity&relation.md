# Entity
## Tag
name String primary key

creation_date_period int (先爬数据，根据爬取结果看时间分布范围，然后给时间分组，这个属性的值就用1，2，3这样的数表示，每个数代表一个范围)

#### 爬取问题，然后爬取标签，每个新标签都用标签名称和创建时间creation_date创建一个tag对象

## Question
question_id String primary key

tags String[] (除了"java"之外其余的标签)

question_creation_date String

answer_count int

#### 标签关联没想好怎么测

## Answer
answer_id String primary key

question_id String foreign key(与Question中的id相同)

score int

answer_delay int (answer_creation_date - question_creation_date, 回答时间与提问时间的差值，天数)

#### 这个entity主要是为第四个分析服务

# Declaration

上面每个主体的属性名称后面都有值的数据类型。目前想法是建立tags, questions, answers三个表，各自的主键和外键可以在上面看到标注。

四个数据分析里面，第一个可以在tags中先group by creation_date_period，然后group by name，然后计数。

第二个可以用questions的tags值，但是具体怎样找到不同标签之间的关联性还没有想好。

第三个涉及到question中具体的内容，还没有想好怎么做。

第四个根据作业要求里面提到的那几个角度，主要分析每个问题的回答情况，所以建立了answer，同时也需要分析问题的具体内容，所以也还没想好怎么做。

### 时间戳转换例子

    public static void main(String[] args) {

        String timestampStr = "3670695200"; // 秒级时间戳，十位

        long timestampSeconds = Long.parseLong(timestampStr);
            
        Instant instantSeconds = Instant.ofEpochSecond(timestampSeconds);
            
        localDateTime localDateTime = LocalDateTime.ofInstant(instantSeconds, ZoneId.systemDefault());
            
        // 格式化为字符串
            
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
        String formatted = localDateTime.format(formatter);
            
        System.out.println("格式化时间: " + formatted);
    
    }