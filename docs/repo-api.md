# 仓库信息管理 API

## 基础路径：`/api/repos`

### 1. 创建仓库

```
POST /api/repos
```

**请求体**:
```json
{
  "name": "ai-code-reviewer",
  "url": "https://github.com/example/ai-code-reviewer.git",
  "branch": "main",
  "description": "AI代码评审平台",
  "language": "Java"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "ai-code-reviewer",
    "url": "https://github.com/example/ai-code-reviewer.git",
    "branch": "main",
    "description": "AI代码评审平台",
    "language": "Java",
    "status": "ACTIVE",
    "createdAt": "2026-07-05T10:00:00",
    "updatedAt": "2026-07-05T10:00:00"
  }
}
```

### 2. 获取仓库列表

```
GET /api/repos
```

### 3. 分页查询仓库

```
GET /api/repos/page?pageNum=1&pageSize=10
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "records": [...]
  }
}
```

### 4. 获取仓库详情

```
GET /api/repos/{id}
```

### 5. 删除仓库

```
DELETE /api/repos/{id}
```