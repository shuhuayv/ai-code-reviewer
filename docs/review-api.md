# 评审任务管理 API

## 基础路径：`/api/reviews`

### 1. 创建评审任务

```
POST /api/reviews/tasks
```

**请求体**:
```json
{
  "repoId": 1,
  "commitId": "abc123def456",
  "branch": "feature/new-feature"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1,
    "repoId": 1,
    "status": "COMPLETED",
    "issueCount": 3,
    "summary": "本次代码评审共发现 3 个问题，其中建议 2 个、警告 1 个...",
    "createdAt": "2026-07-05T10:00:00"
  }
}
```

### 2. 获取评审任务详情

```
GET /api/reviews/tasks/{id}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "repoId": 1,
    "commitId": "abc123def456",
    "branch": "feature/new-feature",
    "status": "COMPLETED",
    "issueCount": 3,
    "createdAt": "2026-07-05T10:00:00",
    "updatedAt": "2026-07-05T10:00:00"
  }
}
```

### 3. 获取评审问题列表

```
GET /api/reviews/tasks/{id}/issues
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "taskId": 1,
      "filePath": "src/main/java/com/example/UserService.java",
      "lineNumber": 45,
      "severity": "SUGGESTION",
      "category": "空值检查",
      "title": "建议对返回值进行空值检查",
      "description": "user.getName() 方法可能返回 null...",
      "suggestion": "建议使用 Optional.ofNullable 包装",
      "createdAt": "2026-07-05T10:00:00"
    }
  ]
}
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
    "summary": "本次代码评审共发现 3 个问题...",
    "overallAssessment": "代码整体质量良好，结构清晰...",
    "createdAt": "2026-07-05T10:00:00"
  }
}
```

## Mock 评审流程

创建评审任务时，系统自动执行以下步骤：

1. 校验 `repoId` 是否存在
2. 创建 `review_task` 记录（状态：COMPLETED）
3. 生成 2-3 条模拟 `review_issue` 记录
4. 生成 `review_report` 记录
5. 返回 `taskId`、`status`、`issueCount`、`summary`