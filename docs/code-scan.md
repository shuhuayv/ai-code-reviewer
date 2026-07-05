# 代码扫描说明

## 概述

代码扫描服务将已克隆到本地的仓库代码文件元数据写入 `code_file` 表，供后续 AI 评审使用。

## 工作流程

1. 根据 `repoId` 查询 `repo_info`
2. 检查 `local_path` 是否存在
3. 删除该仓库的旧 `code_file` 记录
4. 递归遍历 `local_path` 下的所有文件
5. 按规则过滤、统计、写入

## 目录过滤

以下目录会被跳过：

| 目录 | 原因 |
|------|------|
| `.git` | Git 版本控制 |
| `target` | Maven/Gradle 构建输出 |
| `node_modules` | npm 依赖 |
| `dist` | 前端构建输出 |
| `build` | 构建目录 |
| `.idea` | IDE 配置 |
| `.vscode` | 编辑器配置 |

## 文件类型过滤

只扫描以下扩展名的文件：

| 扩展名 | 语言 |
|--------|------|
| `.java` | Java |
| `.py` | Python |
| `.js` | JavaScript |
| `.ts` | TypeScript |
| `.vue` | Vue |
| `.go` | Go |
| `.md` | Markdown |
| `.yml` / `.yaml` | YAML |
| `.xml` | XML |

## 大小限制

单个文件超过 `MAX_FILE_SIZE`（默认 200KB）会被跳过，计入 `skippedFileCount`。

## 统计信息

每个文件记录以下信息：

| 字段 | 说明 |
|------|------|
| `file_path` | 相对仓库根目录的路径 |
| `file_name` | 文件名 |
| `language` | 根据扩展名识别的语言 |
| `content` | 文件完整内容 |
| `char_count` | 字符数 |
| `line_count` | 行数 |
| `content_hash` | SHA-256 哈希值 |

## 当前限制

1. 只支持文本文件，不支持二进制文件
2. 文件内容全量存储，暂不支持按需读取
3. 扫描前会删除旧记录，不支持增量扫描
4. 需要先执行 clone 操作才能扫描
5. 大文件（>200KB）会被跳过