#!/bin/bash
# 数据库初始化脚本
# 用法: bash scripts/init_db.sh

set -e

echo "=========================================="
echo "  AI Code Reviewer - 数据库初始化"
echo "=========================================="

read -p "MySQL Host [localhost]: " DB_HOST
DB_HOST=${DB_HOST:-localhost}

read -p "MySQL Port [3306]: " DB_PORT
DB_PORT=${DB_PORT:-3306}

read -p "MySQL Username [root]: " DB_USER
DB_USER=${DB_USER:-root}

read -s -p "MySQL Password: " DB_PASS
echo ""

echo ""
echo "[1/2] 创建数据库和表..."
mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" < sql/init.sql

echo ""
echo "[2/2] 验证表结构..."
mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" ai_code_reviewer -e "SHOW TABLES;"

echo ""
echo "=========================================="
echo "  数据库初始化完成!"
echo "=========================================="