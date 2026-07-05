#!/bin/bash
# ============================================
# AI Code Reviewer - 数据库迁移脚本
# 用于将旧版本 ai_code_reviewer 数据库升级到当前代码要求的表结构
# 可重复执行，不会删除已有数据
# ============================================
# 用法:
#   export DB_NAME=ai_code_reviewer
#   export DB_USERNAME=root
#   bash scripts/migrate_db.sh
# ============================================

set -e

DB_NAME=${DB_NAME:-ai_code_reviewer}
DB_USERNAME=${DB_USERNAME:-root}

echo "=========================================="
echo "  AI Code Reviewer - 数据库迁移"
echo "=========================================="
echo "数据库: ${DB_NAME}"
echo "用户: ${DB_USERNAME}"
echo ""

# 生成迁移 SQL（只输入一次密码）
SQL_FILE=$(mktemp)
cat > "$SQL_FILE" << 'EOSQL'
USE ai_code_reviewer;

SET @dbname = DATABASE();

-- ============================================
-- repo_info 表
-- ============================================
SELECT COUNT(*) INTO @exists
FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 'repo_info' AND column_name = 'remark';

SET @sql = IF(@exists = 0,
  'ALTER TABLE repo_info ADD COLUMN remark VARCHAR(500) DEFAULT NULL AFTER status',
  'SELECT "repo_info.remark" AS "已存在，跳过"');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- code_file 表
-- ============================================

SELECT COUNT(*) INTO @exists
FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 'code_file' AND column_name = 'file_name';

SET @sql = IF(@exists = 0,
  'ALTER TABLE code_file ADD COLUMN file_name VARCHAR(200) DEFAULT NULL AFTER file_path',
  'SELECT "code_file.file_name" AS "已存在，跳过"');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 'code_file' AND column_name = 'content';

SET @sql = IF(@exists = 0,
  'ALTER TABLE code_file ADD COLUMN content LONGTEXT DEFAULT NULL AFTER language',
  'SELECT "code_file.content" AS "已存在，跳过"');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 'code_file' AND column_name = 'char_count';

SET @sql = IF(@exists = 0,
  'ALTER TABLE code_file ADD COLUMN char_count INT DEFAULT 0 AFTER content',
  'SELECT "code_file.char_count" AS "已存在，跳过"');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 'code_file' AND column_name = 'line_count';

SET @sql = IF(@exists = 0,
  'ALTER TABLE code_file ADD COLUMN line_count INT DEFAULT 0 AFTER char_count',
  'SELECT "code_file.line_count" AS "已存在，跳过"');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 'code_file' AND column_name = 'content_hash';

SET @sql = IF(@exists = 0,
  'ALTER TABLE code_file ADD COLUMN content_hash VARCHAR(64) DEFAULT NULL AFTER line_count',
  'SELECT "code_file.content_hash" AS "已存在，跳过"');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- review_report 表
-- ============================================
SELECT COUNT(*) INTO @exists
FROM information_schema.columns
WHERE table_schema = @dbname AND table_name = 'review_report' AND column_name = 'markdown_content';

SET @sql = IF(@exists = 0,
  'ALTER TABLE review_report ADD COLUMN markdown_content LONGTEXT DEFAULT NULL AFTER overall_assessment',
  'SELECT "review_report.markdown_content" AS "已存在，跳过"');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

EOSQL

echo "执行迁移 SQL..."
mysql -u "${DB_USERNAME}" -p < "$SQL_FILE"
rm -f "$SQL_FILE"

echo ""
echo "=========================================="
echo "  数据库迁移完成!"
echo "=========================================="