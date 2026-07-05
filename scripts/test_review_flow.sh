#!/bin/bash
# Mock 评审流程测试脚本
# 用法: bash scripts/test_review_flow.sh [BASE_URL]

set -e

BASE_URL=${1:-http://localhost:8080}

echo "=========================================="
echo "  AI Code Reviewer - Mock 评审流程测试"
echo "=========================================="
echo "Base URL: ${BASE_URL}"
echo ""

# 1. 创建仓库
echo "[1] 创建仓库..."
REPO_RESP=$(curl -s -X POST "${BASE_URL}/api/repos" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-repo",
    "url": "https://github.com/test/test-repo.git",
    "branch": "main",
    "description": "测试仓库",
    "language": "Java"
  }')
echo "Response: ${REPO_RESP}"
REPO_ID=$(echo "${REPO_RESP}" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "Repo ID: ${REPO_ID}"
echo ""

# 2. 创建评审任务
echo "[2] 创建 Mock 评审任务..."
REVIEW_RESP=$(curl -s -X POST "${BASE_URL}/api/reviews/tasks" \
  -H "Content-Type: application/json" \
  -d "{
    \"repoId\": ${REPO_ID},
    \"commitId\": \"abc123def456\",
    \"branch\": \"feature/test\"
  }")
echo "Response: ${REVIEW_RESP}"
TASK_ID=$(echo "${REVIEW_RESP}" | grep -o '"taskId":[0-9]*' | head -1 | cut -d: -f2)
echo "Task ID: ${TASK_ID}"
echo ""

# 3. 获取任务详情
echo "[3] 获取任务详情..."
curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}" | python3 -m json.tool 2>/dev/null || curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}"
echo ""
echo ""

# 4. 获取问题列表
echo "[4] 获取评审问题列表..."
curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}/issues" | python3 -m json.tool 2>/dev/null || curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}/issues"
echo ""
echo ""

# 5. 获取评审报告
echo "[5] 获取评审报告..."
curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}/report" | python3 -m json.tool 2>/dev/null || curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}/report"
echo ""
echo ""

# 6. 分页查询仓库
echo "[6] 分页查询仓库..."
curl -s "${BASE_URL}/api/repos/page?pageNum=1&pageSize=10" | python3 -m json.tool 2>/dev/null || curl -s "${BASE_URL}/api/repos/page?pageNum=1&pageSize=10"
echo ""
echo ""

echo "=========================================="
echo "  测试完成!"
echo "=========================================="