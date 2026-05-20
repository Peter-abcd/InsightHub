# InsightHub 内容社区实时分析平台

## 项目简介

InsightHub 是基于 Echo 社区系统二次开发的内容社区项目，支持用户登录、发帖、评论、点赞、关注、私信通知、系统通知、全文检索和数据统计等功能。

当前版本重点完成社区基础功能、Kafka 异步通知、Elasticsearch 搜索、Redis 点赞关注模型和开发环境数据生成能力。后续将扩展用户行为事件流、Flink 实时计算和 ClickHouse 分析看板。

## 技术栈

- 后端：Spring Boot、Spring MVC、MyBatis、Spring Security
- 数据库：MySQL 5.7
- 缓存：Redis
- 消息队列：Kafka、Zookeeper
- 搜索引擎：Elasticsearch 6.4.3
- 定时任务：Quartz
- 模板引擎：Thymeleaf
- 开发环境：Docker Compose + IDEA

## 当前功能

- 用户登录与权限认证
- 帖子发布、详情展示、分页查询
- 评论与回复
- 点赞与关注
- 私信与系统通知
- Kafka 异步通知
- Elasticsearch 帖子搜索
- Redis UV / DAU 统计
- 开发环境 Mock 数据生成

## 本地启动方式

### 1. 启动基础依赖

```bash
docker compose up -d
```