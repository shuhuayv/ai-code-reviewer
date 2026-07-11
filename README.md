# AI Code Reviewer

基于大模型的代码评审平台（后端服务）。支持 **Mock 规则评审**（默认，无需 Key）与 **真实智谱 GLM AI 评审**（OpenAI-compatible）双模式；覆盖「创建仓库 → JGit 克隆 → 代码扫描 → 创建评审 → 生成 Markdown 报告」全链路。

> 数据库与接口当前**已恢复正常**：`/api/repos` 正常返回，表名为 `repo_info`（非旧版 `repository`）。

## 真实能力边界（诚实口径）

- **Mock 规则评审**：7 条内置规则（参数校验、事务控制、异常处理、日志、敏感信息、TODO、复杂度），无需外部 API，稳定可重复。
- **真实 AI 评审**：调用智谱 GLM（默认 `glm-4.7-flash`，可经 `AI_MODEL` 覆盖）。**当前真实 AI 仅评审有限文件（默认前 3 个核心文件）、最多少量 issues**，且 JSON 解析链路健壮（content / reasoning_content / Markdown fallback）。
- **不是** 商业级 SaaS、不做 Diff Review、不承诺全量评审或准确率。本地 Demo 无登录 / 权限 / 高并发承诺。

## 技术栈

- Java 21 + Spring Boot 4.1
- MyBatis-Plus 3.5 + MySQL + Redis
- JGit（仓库克隆）
- Spring RestClient（OpenAI-compatible Chat 调用，非 Spring AI）
- Springdoc OpenAPI + Lombok + Jakarta Validation

## 系统架构

```
RepoController → RepoService → JGitRepoServiceImpl(克隆到 repos/{id}, gitignored)
                              → CodeScanServiceImpl(过滤 .git/target/node_modules 等)
ReviewController → ReviewService(Mock/AI 双模式)
                 → CodeReviewAnalysisService(规则驱动 + AI 调用)
                 → CodeReviewModelService: MockCodeReviewModelServiceImpl / OpenAiCompatibleCodeReviewModelServiceImpl
                 → 生成 Markdown 报告(review_report.markdown_content)
```

数据表（MyBatis-Plus，建表见 `sql/init.sql`）：`repo_info` / `code_file` / `review_task` / `review_issue` / `review_report` / `prompt_template`。

## 核心业务流程

1. 创建仓库 `POST /api/repos`
2. 克隆 `POST /api/repos/{id}/clone`（JGit）
3. 扫描 `POST /api/repos/{id}/scan`
4. 创建评审 `POST /api/reviews/tasks`（基于真实扫描代码，输出 Markdown 报告）
5. 查看 `GET /api/reviews/tasks/{id}/issues` 与 `/report`（含 `markdownContent`）、`/report/markdown`

## 本地依赖

- JDK 21+、Maven 3.9+
- MySQL（本机 mysql8 容器，端口 3307）、Redis 7（6379）
- 真实 AI 模式需智谱 Key（Keychain `ai-code-reviewer-db` 存 DB 密码；Keychain 存 AI Key）

## 环境变量

| 变量 | 默认 | 说明 |
|------|------|------|
| `SERVER_PORT` | `8081` | 服务端口 |
| `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD` | 127.0.0.1/3307/ai_code_reviewer/reviewer_app | 数据库（密码从 Keychain 读取，不落地） |
| `AI_MOCK_ENABLED` | `true` | `true`=Mock 规则评审；`false`=真实 AI |
| `AI_PROVIDER` | `zhipu` | 真实 AI provider |
| `AI_MODEL` | `glm-4.7-flash` | 真实 AI 模型 |
| `ZHIPU_API_KEY` / `AI_API_KEY` | 空 | 智谱 Key（不提交） |

## 本地启动

### 1. 数据库初始化

```bash
chmod +x scripts/*.sh
bash scripts/init_db.sh                       # 首次建库建表
# 旧库迁移（Unknown column 时）：bash scripts/migrate_db.sh
```

### 2. 启动服务

```bash
# 推荐：从 Keychain 读密的安全脚本
bash scripts/start_reviewer_local.sh
bash scripts/stop_reviewer_local.sh

# 或传统方式（需先 source .env 提供 DB_PASSWORD）
mvn spring-boot:run
# 或 java -jar target/ai-code-reviewer-1.0.0-SNAPSHOT.jar
```

### 3. 访问地址（注意端口为 8081）

| 服务 | 地址 |
|------|------|
| API 服务 | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |

## 测试命令

```bash
mvn -B test          # 当前 0 个测试（Day 3 将补充仓库/克隆扫描/评审/Markdown/Controller 核心测试）
mvn -B package -DskipTests
```

## CI

GitHub Actions：`.github/workflows/ci.yml`（push/PR main，Java 21，Maven 缓存，`mvn -B test` + `mvn -B package -DskipTests`）。测试用 Mock/H2/Testcontainers，不调用真实 API、不依赖本机真实库。

## API / 页面入口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/repos` | 创建仓库 |
| GET | `/api/repos` `/page` `/{id}` | 仓库列表/分页/详情 |
| POST | `/api/repos/{id}/clone` `/scan` | 克隆 / 扫描 |
| GET | `/api/repos/{id}/files` `/page` | 代码文件 |
| POST | `/api/reviews/tasks` | 创建评审任务（含 Markdown 报告） |
| GET | `/api/reviews/tasks/{id}` `/issues` `/report` `/report/markdown` | 任务/问题/JSON 报告/Markdown 报告 |

## 演示步骤

```bash
bash scripts/demo_review_flow.sh     # 自动串联 创建repo→clone→scan→review→导出报告到 reports/generated/
```

重置 demo 数据（清理 `repo_info`/`review_*`/`code_file` 与本地 `repos/`、`reports/generated/`）：

```bash
bash scripts/reset_demo_data.sh --yes
```

> 注意：脚本与建表均使用 **`repo_info`** 表；旧文档若写 `repository` 为过时表述。

## 评审机制

### Mock 模式（默认）

```bash
export AI_MOCK_ENABLED=true
```

7 条规则检测，无需 API Key。

### 真实 AI 评审模式

```bash
export AI_MOCK_ENABLED=false AI_PROVIDER=zhipu
export ZHIPU_API_KEY='<从 Keychain 读取，绝不提交>'
export AI_MODEL='glm-4.7-flash'
```

OpenAI-compatible Chat Completions 接口，可切换阿里百炼 / DeepSeek / 火山方舟等兼容 provider。

## 已知限制

- 真实 AI 仅评前 3 个核心文件、最多少量 issues；不是全量/商业级评审。
- 不做 Diff Review、不做私有仓库凭证管理。
- 多语言扫描以常见源码目录为主，超大文件/二进制会被跳过。

## 面试亮点

- Mock / 真实 AI 双模式，JSON 解析健壮（多来源 fallback + 校验）。
- JGit 克隆 + 目录过滤（.git/target/node_modules 等）的工程化扫描。
- 数据库与接口已恢复，Keychain 读密、参数化配置，无明文落地。
- 全链路 Markdown 报告导出 + 前端卡片化展示。

## 故障排查

- `405/404 端口不对`：本服务端口为 **8081**（非 8080），确认 `SERVER_PORT` 与前端代理一致。
- `/api/repos` 报 500：检查 mysql8(3307) 是否启动、DB 密码是否经 Keychain 正确注入、`repo_info` 表是否存在（`bash scripts/init_db.sh`）。
- 真实 AI 无结果：`AI_MOCK_ENABLED=false` 且 Key 已配置；受速率限制时稍后重试。

## 文档

[sql/init.sql](sql/init.sql) · [scripts/](scripts/) · Demo 详见 `bash scripts/demo_review_flow.sh`
