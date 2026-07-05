# 配置说明

## 环境变量

复制 `.env.example` 为 `.env` 并修改相应配置：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| DB_NAME | ai_code_reviewer | 数据库名称 |
| DB_USERNAME | ai_dev | 数据库用户名 |
| DB_PASSWORD | (空) | 数据库密码 |
| REDIS_HOST | localhost | Redis 主机 |
| REDIS_PORT | 6379 | Redis 端口 |
| REPO_BASE_DIR | repos | 仓库本地存储目录 |
| MAX_FILE_SIZE | 200000 | 单个文件最大字符数 |
| MAX_TOTAL_CHARS | 60000 | 评审总字符数上限 |
| AI_MOCK_ENABLED | true | 是否启用 Mock 评审 |

## application.yml 关键配置

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/${DB_NAME:ai_code_reviewer}?...
    username: ${DB_USERNAME:ai_dev}
    password: ${DB_PASSWORD:}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
```

## 数据库初始化

```bash
mysql -u root -p < sql/init.sql
```

脚本会创建数据库 `ai_code_reviewer` 和 6 张业务表。

## MyBatis-Plus 自动填充

所有实体的 `created_at` 和 `updated_at` 字段由 MyBatis-Plus 自动填充，无需手动设置。

- `created_at`：insert 时自动填充为当前时间（`FieldFill.INSERT`）
- `updated_at`：insert 和 update 时自动填充为当前时间（`FieldFill.INSERT_UPDATE`）

实现类：[MybatisPlusMetaObjectHandler.java](../src/main/java/com/shuhuayv/codereviewer/config/MybatisPlusMetaObjectHandler.java)

涉及实体：`RepoInfo`、`ReviewTask`、`PromptTemplate`（含 updatedAt），`CodeFile`、`ReviewIssue`、`ReviewReport`（仅 createdAt）