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
| AI_MOCK_ENABLED | true | 是否启用 Mock 评审（false 时使用真实 AI） |
| AI_PROVIDER | mock | AI provider（mock/zhipu） |
| AI_API_KEY | (空) | AI API Key（或 ZHIPU_API_KEY） |
| AI_API_BASE_URL | https://open.bigmodel.cn/api/paas/v4 | AI API 基础地址 |
| AI_MODEL | glm-4.7-flash | AI 模型名称 |
| AI_TIMEOUT_SECONDS | 30 | API 超时（秒） |
| AI_MAX_TOKENS | 2048 | 最大输出 token 数 |
| AI_TEMPERATURE | 0.2 | 生成温度 |

## AI 评审模式

项目支持两种评审模式，通过 `AI_MOCK_ENABLED` 切换：

### Mock 模式（默认）

```bash
export AI_MOCK_ENABLED=true
```

基于 7 条内置规则检测常见问题，无需外部 API Key。

### 智谱 GLM 真实 AI 评审

```bash
export AI_MOCK_ENABLED=false
export AI_PROVIDER=zhipu
export ZHIPU_API_KEY='your_api_key'
export AI_API_BASE_URL='https://open.bigmodel.cn/api/paas/v4'
export AI_MODEL='glm-4.7-flash'
```

真实 AI 评审采用 OpenAI-compatible Chat Completions 接口，后续可切换阿里百炼、DeepSeek、火山方舟等兼容 OpenAI 的 provider。

> **注意**：
> - 不要提交真实 API Key
> - API Key 只能通过环境变量配置
> - 默认 Mock 模式无需配置即可运行

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

首次使用：

```bash
bash scripts/init_db.sh
```

## 数据库迁移

已有旧版本数据库时（出现 `Unknown column` 错误）：

```bash
export DB_NAME=ai_code_reviewer
export DB_USERNAME=root
bash scripts/migrate_db.sh
```

迁移脚本使用 `information_schema` 检测缺失字段并自动添加，可重复执行，不会删除已有数据。

## MyBatis-Plus 自动填充

所有实体的 `created_at` 和 `updated_at` 字段由 MyBatis-Plus 自动填充，无需手动设置。

- `created_at`：insert 时自动填充为当前时间（`FieldFill.INSERT`）
- `updated_at`：insert 和 update 时自动填充为当前时间（`FieldFill.INSERT_UPDATE`）

实现类：[MybatisPlusMetaObjectHandler.java](../src/main/java/com/shuhuayv/codereviewer/config/MybatisPlusMetaObjectHandler.java)

涉及实体：`RepoInfo`、`ReviewTask`、`PromptTemplate`（含 updatedAt），`CodeFile`、`ReviewIssue`、`ReviewReport`（仅 createdAt）