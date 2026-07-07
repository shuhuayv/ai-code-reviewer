#!/bin/bash
# ============================================
# AI Code Reviewer - 一键演示脚本
# 串联完整流程：创建 repo → clone → scan → review → 导出报告
# ============================================
# 用法:
#   bash scripts/demo_review_flow.sh
# ============================================

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
REPO_ID="${REPO_ID:-2}"
REPORTS_DIR="reports/generated"

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo_step() {
    echo -e "\n${BLUE}=========================================="
    echo -e "  $1"
    echo -e "==========================================${NC}"
}

echo_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

echo_info() {
    echo -e "${YELLOW}→ $1${NC}"
}

echo_warn() {
    echo -e "${RED}⚠ $1${NC}"
}

# 检测 AI 模式
AI_MOCK_ENABLED="${AI_MOCK_ENABLED:-true}"
AI_PROVIDER="${AI_PROVIDER:-mock}"
AI_MODEL="${AI_MODEL:-glm-4.7-flash}"

echo "============================================================"
echo "  AI Code Reviewer - 一键演示"
echo "============================================================"
echo ""
if [ "$AI_MOCK_ENABLED" = "false" ]; then
    echo "  AI 模式: 真实 AI 评审"
    echo "  Provider: ${AI_PROVIDER}"
    echo "  Model   : ${AI_MODEL}"
    if [ -z "${ZHIPU_API_KEY:-}" ] && [ -z "${AI_API_KEY:-}" ]; then
        echo_warn "未配置 ZHIPU_API_KEY 或 AI_API_KEY，请设置环境变量"
        echo "  export ZHIPU_API_KEY='your_api_key'"
        exit 1
    fi
else
    echo "  AI 模式: Mock AI（规则驱动）"
fi
echo "============================================================"

# 检查服务是否启动
echo_step "检查服务状态"
if curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/swagger-ui.html" 2>/dev/null | grep -q "200\|301\|302"; then
    echo_success "服务运行中: ${BASE_URL}"
else
    echo_info "服务未启动，请先启动: java -jar target/ai-code-reviewer-1.0.0-SNAPSHOT.jar"
    exit 1
fi

# 1. 创建仓库
echo_step "1. 创建仓库"
CREATE_RESP=$(curl -s -X POST "${BASE_URL}/api/repos" \
    -H "Content-Type: application/json" \
    -d '{
        "name": "demo-repo",
        "url": "https://github.com/spring-projects/spring-petclinic.git",
        "branch": "main",
        "language": "Java"
    }')
echo_info "创建响应: ${CREATE_RESP}"

# 从响应中提取 repoId
REPO_ID=$(echo "$CREATE_RESP" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
if [ -z "$REPO_ID" ]; then
    echo_info "自动解析 repoId 失败，使用默认 REPO_ID=${REPO_ID}"
    REPO_ID="${REPO_ID:-2}"
else
    echo_success "repoId=${REPO_ID}"
fi

# 2. 克隆仓库
echo_step "2. 克隆仓库"
CLONE_RESP=$(curl -s -X POST "${BASE_URL}/api/repos/${REPO_ID}/clone")
echo_info "克隆响应: ${CLONE_RESP}"
echo_success "克隆完成"

# 3. 扫描代码
echo_step "3. 扫描代码文件"
SCAN_RESP=$(curl -s -X POST "${BASE_URL}/api/repos/${REPO_ID}/scan")
echo_info "扫描响应: ${SCAN_RESP}"
echo_success "扫描完成"

# 4. 创建评审任务
echo_step "4. 创建评审任务"
REVIEW_RESP=$(curl -s -X POST "${BASE_URL}/api/reviews/tasks" \
    -H "Content-Type: application/json" \
    -d "{
        \"repoId\": ${REPO_ID},
        \"branch\": \"main\",
        \"commitId\": \"demo-001\",
        \"reviewScope\": \"FULL_REPO\"
    }")
echo_info "评审响应: ${REVIEW_RESP}"

# 从响应中解析 taskId
TASK_ID=$(echo "$REVIEW_RESP" | grep -o '"taskId":[0-9]*' | cut -d: -f2)
if [ -z "$TASK_ID" ]; then
    echo_info "无法自动解析 taskId，请手动输入 taskId:"
    read -r TASK_ID
fi
echo_success "taskId=${TASK_ID}"

# 5. 查询任务详情
echo_step "5. 查询任务详情"
TASK_DETAIL=$(curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}")
echo_info "${TASK_DETAIL}"

# 6. 查询问题列表
echo_step "6. 查询评审问题"
ISSUES=$(curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}/issues")
echo_info "${ISSUES}"

# 7. 查询报告
echo_step "7. 查询评审报告"
REPORT=$(curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}/report")
echo_info "${REPORT}"

# 8. 导出 Markdown 报告
echo_step "8. 导出 Markdown 报告"
mkdir -p "${REPORTS_DIR}"
REPORT_FILE="${REPORTS_DIR}/review-task-${TASK_ID}.md"
curl -s "${BASE_URL}/api/reviews/tasks/${TASK_ID}/report/markdown" -o "${REPORT_FILE}"
echo_success "报告已保存到: ${REPORT_FILE}"

echo ""
echo -e "${GREEN}=========================================="
echo -e "  演示流程完成!"
echo -e "==========================================${NC}"
echo ""
echo "查看报告: cat ${REPORT_FILE}"
echo "浏览器打开: open ${REPORT_FILE}"