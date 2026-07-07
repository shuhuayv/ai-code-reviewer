#!/bin/bash
# ============================================
# AI Code Reviewer - Demo 数据重置脚本
# 清理 demo 演示产生的数据，恢复干净状态
# ============================================
# 用法:
#   bash scripts/reset_demo_data.sh           # 交互确认后执行
#   bash scripts/reset_demo_data.sh --yes     # 跳过确认直接执行
#   bash scripts/reset_demo_data.sh --dry-run # 预览将要清理的内容
#   bash scripts/reset_demo_data.sh --help    # 显示帮助信息
# ============================================

set -e

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

# ============================================
# 参数解析
# ============================================
DRY_RUN=false
SKIP_CONFIRM=false

for arg in "$@"; do
    case "$arg" in
        --help|-h)
            echo "AI Code Reviewer - Demo 数据重置脚本"
            echo ""
            echo "用法:"
            echo "  bash scripts/reset_demo_data.sh          交互确认后执行清理"
            echo "  bash scripts/reset_demo_data.sh --yes    跳过确认直接执行"
            echo "  bash scripts/reset_demo_data.sh --dry-run 预览将要清理的内容"
            echo "  bash scripts/reset_demo_data.sh --help   显示此帮助信息"
            echo ""
            echo "环境变量:"
            echo "  DB_HOST      数据库主机 (默认: localhost)"
            echo "  DB_PORT      数据库端口 (默认: 3306)"
            echo "  DB_NAME      数据库名称 (默认: ai_code_reviewer)"
            echo "  DB_USERNAME  数据库用户名 (默认: ai_dev)"
            echo "  DB_PASSWORD  数据库密码 (默认: Ai_dev_123456)"
            echo ""
            echo "清理范围:"
            echo "  - 本地 repos/ 下 demo 仓库的克隆目录"
            echo "  - 本地 reports/generated/review-task-*.md"
            echo "  - 数据库 review_issue, review_report, review_task, code_file, repository"
            echo "  - 只清理 name='demo-repo' 或 url='https://github.com/spring-projects/spring-petclinic.git' 的仓库数据"
            echo ""
            echo "安全保证:"
            echo "  - 不删除数据库表结构"
            echo "  - 不删除 Git 文件"
            echo "  - 不删除 src、docs、README 等项目文件"
            exit 0
            ;;
        --yes|-y)
            SKIP_CONFIRM=true
            ;;
        --dry-run|-n)
            DRY_RUN=true
            ;;
        *)
            echo_warn "未知参数: $arg"
            echo "使用 --help 查看帮助信息"
            exit 1
            ;;
    esac
done

# ============================================
# 环境变量
# ============================================
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-ai_code_reviewer}"
DB_USERNAME="${DB_USERNAME:-ai_dev}"
DB_PASSWORD="${DB_PASSWORD:-Ai_dev_123456}"

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPOS_DIR="${PROJECT_DIR}/repos"
REPORTS_DIR="${PROJECT_DIR}/reports/generated"

MYSQL_CMD="mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USERNAME} -p${DB_PASSWORD} ${DB_NAME}"

# ============================================
# 函数：扫描本地 repos/ 下的 demo 仓库目录
# 通过 git remote origin url 判断，不依赖数据库
# ============================================
scan_local_repos() {
    LOCAL_REPO_DIRS=""
    if [ ! -d "$REPOS_DIR" ]; then
        return
    fi
    for dir in "$REPOS_DIR"/*/; do
        [ -d "$dir" ] || continue
        local dir_name
        dir_name=$(basename "$dir")
        if [ -d "$dir/.git" ]; then
            local remote_url
            remote_url=$(git -C "$dir" remote get-url origin 2>/dev/null || true)
            if echo "$remote_url" | grep -qi "github.com/spring-projects/spring-petclinic"; then
                if [ -z "$LOCAL_REPO_DIRS" ]; then
                    LOCAL_REPO_DIRS="$dir_name"
                else
                    LOCAL_REPO_DIRS="$LOCAL_REPO_DIRS $dir_name"
                fi
            fi
        fi
    done
}

# ============================================
# 函数：扫描本地 reports/generated/ 下的报告文件
# ============================================
scan_local_reports() {
    LOCAL_REPORT_FILES=""
    if [ -d "$REPORTS_DIR" ]; then
        LOCAL_REPORT_FILES=$(ls "${REPORTS_DIR}"/review-task-*.md 2>/dev/null || true)
    fi
}

# ============================================
# 前置检查
# ============================================
echo "============================================================"
echo "  AI Code Reviewer - Demo 数据重置"
echo "============================================================"
echo ""

if [ "$DRY_RUN" = true ]; then
    echo_info "模式: DRY-RUN（只预览，不实际删除）"
else
    echo_info "模式: 真实执行"
fi
echo ""

# 检查 mysql 命令
if ! command -v mysql &> /dev/null; then
    echo_warn "未找到 mysql 命令，请安装 MySQL 客户端"
    echo "  macOS: brew install mysql-client"
    echo "  Ubuntu: sudo apt install mysql-client"
    echo "  或使用 Docker: docker run --rm mysql:8.0 mysql ..."
    echo ""
    echo "提示: 如果只需要清理本地文件，可以手动删除:"
    echo "  rm -rf repos/ 下的 demo 仓库目录"
    echo "  rm -f reports/generated/review-task-*.md"
    exit 1
fi
echo_success "mysql 命令可用"

# 测试数据库连接
echo_info "连接数据库 ${DB_HOST}:${DB_PORT}/${DB_NAME} ..."
if ! echo "SELECT 1;" | ${MYSQL_CMD} -s --skip-column-names &> /dev/null; then
    echo_warn "数据库连接失败，请检查环境变量 DB_HOST DB_PORT DB_NAME DB_USERNAME DB_PASSWORD"
    echo ""
    echo "提示: 如果只需要清理本地文件，可以手动删除:"
    echo "  rm -rf repos/ 下的 demo 仓库目录"
    echo "  rm -f reports/generated/review-task-*.md"
    exit 1
fi
echo_success "数据库连接成功"

# ============================================
# 1. 查询数据库 demo 仓库数据
# ============================================
echo_step "1. 数据库 demo 数据"

DEMO_REPO_IDS=$(echo "SELECT id FROM repository WHERE name = 'demo-repo' OR url = 'https://github.com/spring-projects/spring-petclinic.git';" \
    | ${MYSQL_CMD} -s --skip-column-names 2>/dev/null || true)

DB_HAS_DATA=false
if [ -z "$DEMO_REPO_IDS" ]; then
    echo_info "数据库中没有 demo 仓库数据"
else
    DB_HAS_DATA=true
    DEMO_REPO_COUNT=$(echo "$DEMO_REPO_IDS" | wc -l | tr -d ' ')
    echo_info "找到 ${DEMO_REPO_COUNT} 个 demo 仓库: $(echo "$DEMO_REPO_IDS" | tr '\n' ' ')"

    REPO_ID_LIST=$(echo "$DEMO_REPO_IDS" | tr '\n' ',' | sed 's/,$//')

    TASK_COUNT=$(echo "SELECT COUNT(*) FROM review_task WHERE repo_id IN (${REPO_ID_LIST});" | ${MYSQL_CMD} -s --skip-column-names 2>/dev/null || echo "0")
    ISSUE_COUNT=$(echo "SELECT COUNT(*) FROM review_issue WHERE task_id IN (SELECT id FROM review_task WHERE repo_id IN (${REPO_ID_LIST}));" | ${MYSQL_CMD} -s --skip-column-names 2>/dev/null || echo "0")
    REPORT_COUNT=$(echo "SELECT COUNT(*) FROM review_report WHERE task_id IN (SELECT id FROM review_task WHERE repo_id IN (${REPO_ID_LIST}));" | ${MYSQL_CMD} -s --skip-column-names 2>/dev/null || echo "0")
    CODE_FILE_COUNT=$(echo "SELECT COUNT(*) FROM code_file WHERE repo_id IN (${REPO_ID_LIST});" | ${MYSQL_CMD} -s --skip-column-names 2>/dev/null || echo "0")

    echo "  关联数据: ${TASK_COUNT} task, ${ISSUE_COUNT} issue, ${REPORT_COUNT} report, ${CODE_FILE_COUNT} code_file"
fi

# ============================================
# 2. 扫描本地 repos/ 目录（独立于数据库）
# ============================================
echo_step "2. 本地 repos/ 目录"

scan_local_repos

if [ -z "$LOCAL_REPO_DIRS" ]; then
    echo_info "未发现本地 demo 仓库目录"
else
    for d in $LOCAL_REPO_DIRS; do
        if [ "$DRY_RUN" = true ]; then
            echo_info "将删除本地 demo 仓库目录：repos/${d}/"
        else
            echo_info "本地 demo 仓库目录：repos/${d}/"
        fi
    done
fi

# ============================================
# 3. 扫描本地报告文件
# ============================================
echo_step "3. 本地报告文件"

scan_local_reports

if [ -z "$LOCAL_REPORT_FILES" ]; then
    echo_info "未发现本地报告文件"
else
    if [ "$DRY_RUN" = true ]; then
        echo "$LOCAL_REPORT_FILES" | while IFS= read -r f; do
            echo_info "将删除报告文件：$(basename "$f")"
        done
    else
        echo "$LOCAL_REPORT_FILES" | while IFS= read -r f; do
            echo_info "本地报告文件：$(basename "$f")"
        done
    fi
fi

# ============================================
# 判断是否有任何可清理内容
# ============================================
HAS_ANYTHING=false
if [ "$DB_HAS_DATA" = true ] || [ -n "$LOCAL_REPO_DIRS" ] || [ -n "$LOCAL_REPORT_FILES" ]; then
    HAS_ANYTHING=true
fi

# ============================================
# DRY-RUN 模式：预览并结束
# ============================================
if [ "$DRY_RUN" = true ]; then
    echo ""
    echo_step "DRY-RUN 完成"
    if [ "$HAS_ANYTHING" = true ]; then
        echo_info "以上是将要清理的内容，实际未执行删除操作"
        echo_info "去掉 --dry-run 参数即可执行真实清理"
        echo ""
        echo -e "${GREEN}Demo 数据重置预览完成。${NC}"
    else
        echo -e "${GREEN}未发现需要清理的 demo 数据。${NC}"
    fi
    exit 0
fi

# ============================================
# 真实执行：无任何可清理内容则提前退出
# ============================================
if [ "$HAS_ANYTHING" = false ]; then
    echo ""
    echo -e "${GREEN}未发现需要清理的 demo 数据。${NC}"
    exit 0
fi

# ============================================
# 用户确认
# ============================================
if [ "$SKIP_CONFIRM" = false ]; then
    echo ""
    echo_warn "即将清理以上所有 demo 数据，此操作不可撤销！"
    read -r -p "确认执行清理? (输入 y 或 yes 确认): " CONFIRM
    if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "yes" ]; then
        echo_info "已取消，未执行任何清理"
        exit 0
    fi
    echo ""
fi

# ============================================
# 执行清理：本地 repos/
# ============================================
echo_step "清理本地 demo 仓库目录"

if [ -n "$LOCAL_REPO_DIRS" ]; then
    for d in $LOCAL_REPO_DIRS; do
        if [ -d "${REPOS_DIR}/${d}" ]; then
            echo_info "删除目录: repos/${d}/"
            rm -rf "${REPOS_DIR}/${d}"
            echo_success "已删除 repos/${d}/"
        fi
    done
else
    echo_info "没有需要清理的本地 demo 仓库目录"
fi

# ============================================
# 执行清理：本地报告文件
# ============================================
echo_step "清理本地报告文件"

if [ -n "$LOCAL_REPORT_FILES" ]; then
    REPORT_COUNT_LOCAL=$(echo "$LOCAL_REPORT_FILES" | wc -l | tr -d ' ')
    echo_info "删除 ${REPORT_COUNT_LOCAL} 个报告文件"
    rm -f "${REPORTS_DIR}"/review-task-*.md
    echo_success "已删除 ${REPORT_COUNT_LOCAL} 个报告文件"
else
    echo_info "没有需要清理的报告文件"
fi

# ============================================
# 执行清理：数据库（按外键安全顺序）
# ============================================
if [ "$DB_HAS_DATA" = true ]; then
    echo_step "清理数据库 demo 数据"

    echo_info "删除 review_issue ..."
    echo "DELETE FROM review_issue WHERE task_id IN (SELECT id FROM review_task WHERE repo_id IN (${REPO_ID_LIST}));" \
        | ${MYSQL_CMD} 2>/dev/null || echo_warn "review_issue 删除失败或无数据"
    echo_success "review_issue 清理完成"

    echo_info "删除 review_report ..."
    echo "DELETE FROM review_report WHERE task_id IN (SELECT id FROM review_task WHERE repo_id IN (${REPO_ID_LIST}));" \
        | ${MYSQL_CMD} 2>/dev/null || echo_warn "review_report 删除失败或无数据"
    echo_success "review_report 清理完成"

    echo_info "删除 review_task ..."
    echo "DELETE FROM review_task WHERE repo_id IN (${REPO_ID_LIST});" \
        | ${MYSQL_CMD} 2>/dev/null || echo_warn "review_task 删除失败或无数据"
    echo_success "review_task 清理完成"

    echo_info "删除 code_file ..."
    echo "DELETE FROM code_file WHERE repo_id IN (${REPO_ID_LIST});" \
        | ${MYSQL_CMD} 2>/dev/null || echo_warn "code_file 删除失败或无数据"
    echo_success "code_file 清理完成"

    echo_info "删除 repository ..."
    echo "DELETE FROM repository WHERE id IN (${REPO_ID_LIST});" \
        | ${MYSQL_CMD} 2>/dev/null || echo_warn "repository 删除失败或无数据"
    echo_success "repository 清理完成"
else
    echo_step "清理数据库 demo 数据"
    echo_info "数据库中没有 demo 数据，跳过"
fi

# ============================================
# 完成
# ============================================
echo ""
echo -e "${GREEN}=========================================="
echo -e "  Demo 数据重置完成!"
echo -e "==========================================${NC}"
echo ""
echo "已清理内容:"
if [ "$DB_HAS_DATA" = true ]; then
    echo "  - 数据库: ${DEMO_REPO_COUNT} 个 repository, ${TASK_COUNT} 个 task, ${ISSUE_COUNT} 个 issue, ${REPORT_COUNT} 个 report, ${CODE_FILE_COUNT} 个 code_file"
fi
if [ -n "$LOCAL_REPO_DIRS" ]; then
    echo "  - 本地目录: repos/ 下的 demo 仓库目录"
fi
if [ -n "$LOCAL_REPORT_FILES" ]; then
    echo "  - 报告文件: reports/generated/review-task-*.md"
fi
echo ""
echo "现在可以重新运行 demo: bash scripts/demo_review_flow.sh"