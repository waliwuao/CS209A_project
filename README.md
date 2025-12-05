# StackOverflowflow 数据采集与存储项目

## 项目概述

该项目旨在通过 Stack Exchange API 采集 Stack Overflow 平台上的问答数据，并将其存储到 PostgreSQL 数据库中，方便后续的数据分析和研究。项目包含数据采集、数据处理和数据库存储等功能模块，主要聚焦于 Java 相关的问答内容。

## 技术栈

- **编程语言**：Java
- **数据库**：PostgreSQL
- **API**：Stack Exchange API v2.3
- **JSON 处理**：Gson
- **HTTP 客户端**：Java 内置 HttpClient
- **JDBC**：用于数据库连接和操作

## 数据库设计

项目采用以下数据库表结构存储 Stack Overflow 数据：

1. **question** - 存储问题信息
   - question_id (主键)
   - account_id - 提问者账号ID
   - accept_answer_id - 被采纳答案ID
   - view_count - 浏览次数
   - answer_count - 回答数量
   - score - 问题得分
   - creation_date - 创建时间(秒级时间戳)
   - title - 问题标题
   - body - 问题内容

2. **answer** - 存储答案信息
   - answer_id (主键)
   - question_id (外键) - 关联的问题ID
   - account_id - 回答者账号ID
   - score - 答案得分
   - creation_date - 创建时间(秒级时间戳)
   - body - 答案内容

3. **tag** - 存储标签信息
   - tag_id (主键，自增)
   - tag_name - 标签名称

4. **question_tag** - 问题与标签的关联表
   - question_id (外键)
   - tag_id (外键)

5. **account** - 存储用户账号信息
   - account_id (主键)
   - reputation - 用户声望值

## 主要功能

1. **数据采集**：通过 Stack Exchange API 获取指定条件的问答数据
2. **数据处理**：解析 API 响应数据，转换为适合数据库存储的格式
3. **数据存储**：将处理后的数据批量导入到 PostgreSQL 数据库
4. **批量处理**：支持批量获取和导入数据，提高处理效率
5. **重试机制**：网络请求失败时具有重试功能，提高稳定性

## 核心类说明

1. **StackExchangeApiToPostgres**：
   - 从 Stack Exchange API 采集问题数据
   - 按月份范围获取数据，支持指定标签(默认为"java")
   - 将问题、用户和标签数据导入数据库

2. **StackExchangeDbToAnswer**：
   - 从数据库中读取已存储的问题ID
   - 批量获取这些问题的答案数据
   - 将答案数据导入到数据库

3. **数据模型类**：
   - QuestionItem：问题信息模型
   - AnswerItem：答案信息模型
   - Owner：用户信息模型
   - StackOverflowData：API 响应数据包装类

## 使用方法

1. **环境准备**：
   - 安装 PostgreSQL 数据库
   - 创建名为 `cs209a_project` 的数据库
   - 执行 `structure_design.md` 中的 SQL 语句创建表结构

2. **配置修改**：
   - 修改代码中的数据库连接信息(DB_URL, USER, PASSWORD)
   - 根据需要调整 API 请求参数(标签、时间范围等)

3. **运行程序**：
   - 先运行 `StackExchangeApiToPostgres` 采集问题数据
   - 再运行 `StackExchangeDbToAnswer` 采集对应答案数据

## 注意事项

1. **API 限制**：Stack Exchange API 有请求频率限制，程序中已添加延迟控制
2. **批量处理**：为避免超出 API 限制，程序采用批量处理机制
3. **冲突处理**：数据库插入操作使用 `ON CONFLICT DO NOTHING` 避免重复数据
4. **重试机制**：网络请求失败时会自动重试，最多重试 3 次

## 扩展建议

1. 可根据需要修改 `generateMonthDateRanges` 方法调整数据采集的时间范围
2. 更改 `TAGGED` 常量可以采集其他标签的问答数据
3. 可添加日志系统替代 System.out 输出
4. 可增加数据校验和清洗功能，提高数据质量
5. 可添加配置文件，避免硬编码数据库信息和 API 参数
