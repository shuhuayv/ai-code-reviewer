# AI Code Reviewer

基于大模型的代码评审平台（后端服务）。

## 技术栈

- **Java 21** + **Spring Boot 4.1.0**
- **MyBatis-Plus 3.5.16** ORM
- **MySQL** 数据库
- **Redis** 缓存
- **JGit** 仓库克隆
- **SpringDoc OpenAPI** Swagger 文档
- **Lombok** 代码简化
- **Jakarta Validation** 参数校验

## 快速开始

### 1. 环境准备

- JDK 21+
- MySQL 8.0+
- Redis 7.0+
- Maven 3.9+

### 2. 数据库初始化与迁移

**首次使用**：

```bash
chmod +x scripts/*.sh
bash scripts/init_db.sh
```

**已有旧版本数据库**（出现 Unknown column 错误时）：

```bash
export DB_NAME=ai_code_reviewer
export DB_USERNAME=root
bash scripts/migrate_db.sh
```

### 3. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 填写数据库密码等配置
```

### 4. 启动服务

```bash
mvn clean package -DskipTests
java -jar target/ai-code-reviewer-1.0.0-SNAPSHOT.jar
```

或使用 Maven 插件：

```bash
mvn spring-boot:run
```

### 5. 访问地址

| 服务 | 地址 |
|------|------|
| API 服务 | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

## 项目结构

```
src/main/java/com/shuhuayv/codereviewer/
├── AiCodeReviewerApplication.java    # 入口
├── common/
│   ├── ApiResponse.java              # 统一响应
│   └── PageResult.java               # 分页结果
├── config/
│   ├── OpenApiConfig.java            # Swagger 配置
│   └── MybatisPlusMetaObjectHandler.java  # 自动填充
├── controller/
│   ├── RepoController.java           # 仓库接口（CRUD + 克隆 + 扫描）
│   └── ReviewController.java         # 评审任务接口
├── dto/
│   ├── CreateRepoRequest.java        # 创建仓库请求
│   ├── CreateReviewTaskRequest.java  # 创建评审请求
│   ├── RepoResponse.java             # 仓库响应
│   ├── RepoCloneResponse.java        # 克隆响应
│   ├── CodeScanResponse.java         # 扫描响应
│   ├── CodeFileResponse.java         # 代码文件响应
│   ├── ReviewTaskResponse.java       # 评审任务响应
│   ├── ReviewTaskDetailResponse.java # 评审任务详情
│   ├── ReviewIssueResponse.java      # 评审问题响应
│   └── ReviewReportResponse.java     # 评审报告响应
├── entity/
│   ├── RepoInfo.java                 # 仓库信息实体
│   ├── CodeFile.java                 # 代码文件实体
│   ├── ReviewTask.java               # 评审任务实体
│   ├── ReviewIssue.java              # 评审问题实体
│   ├── ReviewReport.java             # 评审报告实体
│   └── PromptTemplate.java           # Prompt模板实体
├── exception/
│   ├── BusinessException.java        # 业务异常
│   └── GlobalExceptionHandler.java   # 全局异常处理
├── mapper/
│   ├── RepoInfoMapper.java           # 仓库 Mapper
│   ├── CodeFileMapper.java           # 代码文件 Mapper
│   ├── ReviewTaskMapper.java         # 评审任务 Mapper
│   ├── ReviewIssueMapper.java        # 评审问题 Mapper
│   ├── ReviewReportMapper.java       # 评审报告 Mapper
│   └── PromptTemplateMapper.java     # Prompt模板 Mapper
├── service/
    ├── RepoService.java              # 仓库服务
    ├── ReviewService.java            # 评审服务（规则驱动 + fallback）
    ├── CodeReviewAnalysisService.java  # 代码评审分析（7 条规则）
    ├── GitRepoService.java           # Git 克隆接口
    ├── CodeScanService.java          # 代码扫描接口
    └── impl/
        ├── JGitRepoServiceImpl.java  # JGit 克隆实现
        └── CodeScanServiceImpl.java  # 代码扫描实现
```

## API 概览

### 仓库信息管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/repos` | 创建仓库 |
| GET | `/api/repos` | 仓库列表 |
| GET | `/api/repos/page?pageNum=1&pageSize=10` | 分页查询 |
| GET | `/api/repos/{id}` | 仓库详情 |
| DELETE | `/api/repos/{id}` | 删除仓库 |
| POST | `/api/repos/{id}/clone` | 克隆仓库（JGit） |
| POST | `/api/repos/{id}/scan` | 扫描代码文件 |
| GET | `/api/repos/{id}/files` | 代码文件列表 |
| GET | `/api/repos/{id}/files/page?pageNum=1&pageSize=10` | 分页查询代码文件 |

### 评审任务管理（规则驱动 + fallback Mock）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/reviews/tasks` | 创建评审任务（含 Markdown 报告） |
| GET | `/api/reviews/tasks/{id}` | 任务详情 |
| GET | `/api/reviews/tasks/{id}/issues` | 问题列表 |
| GET | `/api/reviews/tasks/{id}/report` | 评审报告（含 markdownContent） |
| GET | `/api/reviews/tasks/{id}/report/markdown` | 导出 Markdown 报告（纯文本） |

## 一键演示

```bash
chmod +x scripts/*.sh
bash scripts/demo_review_flow.sh
```

脚本自动串联：创建 repo → clone → scan → review → 导出 Markdown 报告到 `reports/generated/`。

## 完整演示链路

```bash
# 1. 创建仓库
curl -X POST http://localhost:8080/api/repos \
  -H "Content-Type: application/json" \
  -d '{"name":"demo","url":"https://github.com/user/repo.git","branch":"main","language":"Java"}'

# 2. 克隆仓库
curl -X POST http://localhost:8080/api/repos/1/clone

# 3. 扫描代码文件
curl -X POST http://localhost:8080/api/repos/1/scan

# 4. 创建评审任务（基于真实扫描代码）
curl -X POST http://localhost:8080/api/reviews/tasks \
  -H "Content-Type: application/json" \
  -d '{"repoId":1,"reviewScope":"FULL_REPO"}'

# 5. 查看问题列表
curl http://localhost:8080/api/reviews/tasks/1/issues

# 6. 查看 JSON 报告
curl http://localhost:8080/api/reviews/tasks/1/report

# 7. 导出 Markdown 报告
curl http://localhost:8080/api/reviews/tasks/1/report/markdown -o review-report.md
```

## 评审机制

- **有扫描数据时**：基于 `code_file` 表中的真实代码内容，执行 7 条规则检测，生成 Markdown 报告
- **无扫描数据时**：回退到固定 3 条 Mock issue，确保接口始终可用
- **当前为规则驱动 Mock AI**，未接入真实大模型 API。后续可替换为真实 AI 服务

## 注意事项

- 评审任务为 **规则驱动 + fallback Mock**，未接入真实大模型 API
- 仓库克隆使用 **JGit**，克隆到 `repos/{repoId}` 目录（已在 .gitignore 中）
- 代码扫描自动过滤 `.git`、`target`、`node_modules` 等目录
- 生产环境请配置真实数据库密码和 Redis 密码
- 不要将 `.env` 文件提交到版本控制