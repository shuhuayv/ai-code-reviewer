# 数据库设计文档

## 概述

使用 MySQL 8.0+，MyBatis-Plus 作为 ORM 框架。

## 表结构

### 1. repo_info（仓库信息表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(200) | NOT NULL | 仓库名称 |
| url | VARCHAR(500) | NOT NULL | 仓库URL |
| branch | VARCHAR(100) | NOT NULL, DEFAULT 'main' | 默认分支 |
| description | VARCHAR(1000) | | 仓库描述 |
| language | VARCHAR(50) | | 主要编程语言 |
| local_path | VARCHAR(500) | | 本地克隆路径 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE/CLONED/FAILED/INACTIVE |
| remark | VARCHAR(500) | | 状态备注（克隆结果等） |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | | 更新时间 |

### 2. code_file（代码文件表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| repo_id | BIGINT | NOT NULL | 关联仓库ID |
| file_path | VARCHAR(500) | NOT NULL | 文件相对路径 |
| file_name | VARCHAR(200) | NOT NULL | 文件名 |
| language | VARCHAR(50) | | 文件语言 |
| content | LONGTEXT | | 文件内容 |
| char_count | INT | DEFAULT 0 | 字符数 |
| line_count | INT | DEFAULT 0 | 行数 |
| content_hash | VARCHAR(64) | | SHA-256 哈希值 |
| created_at | DATETIME | NOT NULL | 创建时间 |

> 文件内容由 `CodeScanService` 扫描本地仓库后写入，`content_hash` 用于后续变更检测。

### 3. review_task（评审任务表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| repo_id | BIGINT | NOT NULL | 关联仓库ID |
| commit_id | VARCHAR(100) | | 提交ID |
| branch | VARCHAR(100) | NOT NULL | 评审分支 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | PENDING/RUNNING/COMPLETED/FAILED |
| issue_count | INT | DEFAULT 0 | 问题数量 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | | 更新时间 |

### 4. review_issue（评审问题表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| task_id | BIGINT | NOT NULL | 关联评审任务ID |
| file_path | VARCHAR(500) | | 文件路径 |
| line_number | INT | | 行号 |
| severity | VARCHAR(20) | NOT NULL, DEFAULT 'SUGGESTION' | SUGGESTION/WARNING/ERROR |
| category | VARCHAR(50) | | 问题分类 |
| title | VARCHAR(500) | NOT NULL | 问题标题 |
| description | TEXT | | 问题描述 |
| suggestion | TEXT | | 修改建议 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 5. review_report（评审报告表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| task_id | BIGINT | NOT NULL, UNIQUE | 关联评审任务ID |
| summary | TEXT | | 评审摘要 |
| overall_assessment | TEXT | | 总体评价 |
| created_at | DATETIME | NOT NULL | 创建时间 |

### 6. prompt_template（Prompt 模板表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(200) | NOT NULL | 模板名称 |
| template_type | VARCHAR(50) | NOT NULL | REVIEW/RETRY |
| content | TEXT | NOT NULL | 模板内容 |
| enabled | TINYINT | NOT NULL, DEFAULT 1 | 0-禁用/1-启用 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | | 更新时间 |

## 状态流转（Mock 模式）

评审任务创建后直接设为 COMPLETED 状态，同步生成问题和报告。

```
PENDING → COMPLETED
```

> 生产环境应改为 PENDING → RUNNING → COMPLETED/FAILED 的异步流程。