# 评审任务管理 API

## 基础路径：`/api/reviews`

### 1. 创建评审任务

```
POST /api/reviews/tasks
```

**请求体**:
```json
{
  "repoId": 2,
  "commitId": "local-scan-001",
  "branch": "main",
  "reviewScope": "FULL_REPO"
}
```

> `commitId`、`branch`、`reviewScope` 均为可选。`branch` 为空时默认使用 `repo_info.branch`，`commitId` 为空时默认使用 `mock-commit`，`reviewScope` 为空时默认 `FULL_REPO`。

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1,
    "repoId": 2,
    "status": "COMPLETED",
    "issueCount": 5,
    "summary": "本次代码评审共发现 5 个问题，其中警告 2 个、建议 3 个。",
    "createdAt": "2026-07-05T10:00:00"
  }
}
```

### 2. 获取评审任务详情

```
GET /api/reviews/tasks/{id}
```

### 3. 获取评审问题列表

```
GET /api/reviews/tasks/{id}/issues
```

### 4. 获取评审报告

```
GET /api/reviews/tasks/{id}/report
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "taskId": 1,
    "summary": "本次代码评审共发现 5 个问题...",
    "overallAssessment": "代码整体质量良好...",
    "markdownContent": "# AI Code Review Report\n\n## 1. Repository Summary\n...",
    "createdAt": "2026-07-05T10:00:00"
  }
}
```

## 评审机制

### 基于已扫描代码（优先）

如果 `repoId` 对应的仓库已通过 `POST /api/repos/{id}/scan` 扫描并写入 `code_file` 表，则评审任务会：

1. 读取该仓库的 `code_file` 数据
2. 优先分析 Java 核心包文件（controller、service、config、exception、entity、dto、mapper、common），其他语言文件做基础检测
3. 跳过 Markdown、YAML、XML 文件
4. 最多分析 20 个文件
5. 执行 7 条规则检测，生成 `review_issue`
6. 生成包含完整 Markdown 格式的 `review_report`

### Fallback Mock（无扫描数据时）

如果 `repoId` 下没有 `code_file` 数据，则回退到固定 3 条 Mock issue，确保评审接口始终可用。

## 规则检测清单

| 规则 | 检测条件 | 严重度 | 类别 |
|------|---------|--------|------|
| 入参校验 | Controller 方法参数缺少 @Valid/@Validated | WARNING | PARAM_VALIDATION |
| 事务控制 | Service 多表写操作缺少 @Transactional | WARNING | TRANSACTION |
| 异常处理 | 直接 throw RuntimeException 或 catch 吞噬异常 | SUGGESTION | EXCEPTION_HANDLING |
| 日志记录 | Service/Controller 缺少日志 | SUGGESTION | LOGGING |
| 安全配置 | 代码中出现硬编码密码/密钥 | WARNING | SECURITY |
| TODO 标记 | 代码包含 TODO/FIXME/HACK/XXX | SUGGESTION | TODO |
| 代码复杂度 | 单文件超过 150 行 | SUGGESTION | MAINTAINABILITY |

## 完整演示链路

```
1. POST /api/repos           → 创建仓库
2. POST /api/repos/{id}/clone → 克隆仓库
3. POST /api/repos/{id}/scan  → 扫描代码文件
4. POST /api/reviews/tasks    → 创建评审任务（基于真实扫描代码）
5. GET  /api/reviews/tasks/{id}/issues  → 查看问题列表
6. GET  /api/reviews/tasks/{id}/report  → 查看 Markdown 报告
```