-- ============================================
-- AI Code Reviewer 数据库初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS ai_code_reviewer
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_code_reviewer;

-- 1. 仓库信息表
CREATE TABLE IF NOT EXISTS repo_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '仓库名称',
    url VARCHAR(500) NOT NULL COMMENT '仓库URL',
    branch VARCHAR(100) NOT NULL DEFAULT 'main' COMMENT '默认分支',
    description VARCHAR(1000) COMMENT '仓库描述',
    language VARCHAR(50) COMMENT '主要编程语言',
    local_path VARCHAR(500) COMMENT '本地克隆路径',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/CLONED/FAILED/INACTIVE',
    remark VARCHAR(500) COMMENT '状态备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_language (language)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仓库信息表';

-- 2. 代码文件表
CREATE TABLE IF NOT EXISTS code_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    repo_id BIGINT NOT NULL COMMENT '关联仓库ID',
    file_path VARCHAR(500) NOT NULL COMMENT '文件相对路径',
    file_name VARCHAR(200) NOT NULL COMMENT '文件名',
    language VARCHAR(50) COMMENT '文件语言',
    content LONGTEXT COMMENT '文件内容',
    char_count INT DEFAULT 0 COMMENT '字符数',
    line_count INT DEFAULT 0 COMMENT '行数',
    content_hash VARCHAR(64) COMMENT 'SHA-256 哈希值',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_repo_id (repo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码文件表';

-- 3. 评审任务表
CREATE TABLE IF NOT EXISTS review_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    repo_id BIGINT NOT NULL COMMENT '关联仓库ID',
    commit_id VARCHAR(100) COMMENT '提交ID',
    branch VARCHAR(100) NOT NULL COMMENT '评审分支',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/COMPLETED/FAILED',
    issue_count INT DEFAULT 0 COMMENT '问题数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_repo_id (repo_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评审任务表';

-- 4. 评审问题表
CREATE TABLE IF NOT EXISTS review_issue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '关联评审任务ID',
    file_path VARCHAR(500) COMMENT '文件路径',
    line_number INT COMMENT '行号',
    severity VARCHAR(20) NOT NULL DEFAULT 'SUGGESTION' COMMENT '严重程度: SUGGESTION/WARNING/ERROR',
    category VARCHAR(50) COMMENT '问题分类',
    title VARCHAR(500) NOT NULL COMMENT '问题标题',
    description TEXT COMMENT '问题描述',
    suggestion TEXT COMMENT '修改建议',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评审问题表';

-- 5. 评审报告表
CREATE TABLE IF NOT EXISTS review_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '关联评审任务ID',
    summary TEXT COMMENT '评审摘要',
    overall_assessment TEXT COMMENT '总体评价',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评审报告表';

-- 6. Prompt 模板表
CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(50) NOT NULL COMMENT '模板类型: REVIEW/RETRY',
    content TEXT NOT NULL COMMENT '模板内容',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用/1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_type_enabled (template_type, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt模板表';