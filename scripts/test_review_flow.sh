#!/bin/bash
# AI Code Reviewer 流程测试脚本
# 用法: bash scripts/test_review_flow.sh [BASE_URL]

set -e

BASE_URL=${1:-http://localhost:8080}

echo "=========================================="
echo "  AI Code Reviewer - 流程测试"
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

# 2. 克隆仓库（可选，如果网络不通会报错，可以跳过）
echo "[2] 克隆仓库（JGit）..."
echo "注意：如果外网无法访问 GitHub，克隆会失败，可跳过此步骤"
CLONE_RESP=$(curl -s -X POST "${BASE_URL}/api/repos/${REPO_ID}/clone" || echo "clone failed")
echo "Response: ${CLONE_RESP}"
echo ""

# 3. 扫描代码文件（需要先克隆成功）
echo "[3] 扫描代码文件..."
echo "注意：需要先克隆成功，否则会报错"
SCAN_RESP=$(curl -s -X POST "${BASE_URL}/api/repos/${REPO_ID}/scan" || echo "scan failed")
echo "Response: ${SCAN_RESP}"
echo ""

# 4. 创建 Mock 评审任务（不需要 clone/scan，可直接测试）
echo "[4] 创建 Mock 评审任务（简化请求，只传 repoId）..."
REVIEW_RESP=$(curl -s -X POST "${BASE_URL}/api/reviews/tasks" \
  -H "Content-Type: application/json" \
  -d "{
    \"repoId\": ${REPO_ID},
    \"reviewScope\": \"FULL_REPO\"
  }")
echo "Response: ${REVIEW_RESP}"
TASK_ID=$(echo "${REVIEW_RESP}" | grep -o '"taskId":[0-9]*' | head -1 | cut -d: -f2)
echo "Task ID: ${TASK_ID}"
echo ""

# 5. 获取任务详情
echo "[5] 获取任务详情..."
curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}" | python3 -m json.tool 2>/dev/null || curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}"
echo ""
echo ""

# 6. 获取问题列表
echo "[6] 获取评审问题列表..."
curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}/issues" | python3 -m json.tool 2>/dev/null || curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}/issues"
echo ""
echo ""

# 7. 获取评审报告
echo "[7] 获取评审报告..."
curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}/report" | python3 -m json.tool 2>/dev/null || curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}/report"
echo ""
echo ""

# 8. 分页查询仓库
echo "[8] 分页查询仓库..."
curl -s "${BASE_URL}/api/repos/page?pageNum=1&pageSize=10" | python3 -m json.tool 2>/dev/null || curl -s "${BASE_URL}/api/repos/page?pageNum=1&pageSize=10"
echo ""
echo ""

# 9. 查询代码文件（如果扫描成功）
echo "[9] 查询代码文件..."
curl -s "${BASE_URL}/api/repos/${REPO_ID}/files" | python3 -m json.tool 2>/dev/null || curl -s "${BASE_URL}/api/repos/${REPO_ID}/files"
echo ""
echo ""

echo "=========================================="
echo "  测试完成!"
echo "  如果 clone 失败，可以先只测 Mock 评审接口"
echo "  Mock 评审接口不需要 clone/scan 即可使用"
echo "=========================================="