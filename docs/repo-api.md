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

### 2. 获取仓库列表

```
GET /api/repos
```

### 3. 分页查询仓库

```
GET /api/repos/page?pageNum=1&pageSize=10
```

### 4. 获取仓库详情

```
GET /api/repos/{id}
```

### 5. 删除仓库

```
DELETE /api/repos/{id}
```

### 6. 克隆仓库

```
POST /api/repos/{id}/clone
```

使用 JGit 将远程仓库克隆到本地 `repos/{id}` 目录。

**前置条件**：仓库记录已创建，`url` 为有效的远程 Git 地址。

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "repoId": 1,
    "repoUrl": "https://github.com/example/repo.git",
    "branch": "main",
    "localPath": "repos/1",
    "status": "CLONED",
    "message": "clone success",
    "costMs": 3500
  }
}
```

克隆成功后，`repo_info.status` 更新为 `CLONED`，`local_path` 设置为本地路径。  
克隆失败时，`status` 更新为 `FAILED`，`remark` 记录失败原因。

### 7. 扫描代码文件

```
POST /api/repos/{id}/scan
```

扫描本地仓库目录，将代码文件元数据写入 `code_file` 表。

**前置条件**：仓库已克隆（`local_path` 存在且有效）。

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "repoId": 1,
    "scannedFileCount": 42,
    "skippedFileCount": 5,
    "totalLineCount": 3850,
    "languages": {
      "Java": 20,
      "Python": 10,
      "YAML": 3
    },
    "costMs": 1200
  }
}
```

### 8. 代码文件列表

```
GET /api/repos/{id}/files
```

### 9. 分页查询代码文件

```
GET /api/repos/{id}/files/page?pageNum=1&pageSize=10
```