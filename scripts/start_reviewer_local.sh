#!/bin/bash
set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="$PROJECT_DIR/.demo-run/logs"
PID_FILE="$PROJECT_DIR/.demo-run/reviewer.pid"
mkdir -p "$LOG_DIR" "$(dirname "$PID_FILE")"

DB_PASSWORD="$(security find-generic-password -a reviewer_app -s ai-code-reviewer-db -w 2>/dev/null || true)"
if [[ -z "$DB_PASSWORD" ]]; then
  printf "请输入 Reviewer 数据库密码："
  IFS= read -r -s DB_PASSWORD
  printf "\n"
fi

if ! docker inspect mysql8 >/dev/null 2>&1; then
  echo "未找到 mysql8 容器" >&2
  exit 1
fi
if [[ "$(docker inspect -f '{{.State.Running}}' mysql8)" != "true" ]]; then
  docker start mysql8 >/dev/null
fi

for pid in $(lsof -ti tcp:8081 2>/dev/null || true); do
  command_line="$(ps -p "$pid" -o command= 2>/dev/null || true)"
  if [[ "$command_line" == *"ai-code-reviewer"* ]] || [[ "$command_line" == *"spring-boot:run"* ]]; then
    kill "$pid" 2>/dev/null || true
    sleep 2
  else
    echo "端口 8081 被其他程序占用：PID=$pid $command_line" >&2
    exit 1
  fi
done

cd "$PROJECT_DIR"
jar_file=""
for candidate in target/ai-code-reviewer-*.jar; do
  [[ -f "$candidate" ]] || continue
  [[ "$candidate" == *.original ]] && continue
  jar_file="$candidate"
  break
done
[[ -n "$jar_file" ]] || { echo "未找到构建后的 Reviewer JAR" >&2; exit 1; }

nohup env \
  SERVER_PORT=8081 \
  DB_HOST=127.0.0.1 \
  DB_PORT=3307 \
  DB_NAME=ai_code_reviewer \
  DB_USERNAME=reviewer_app \
  DB_PASSWORD="$DB_PASSWORD" \
  AI_MOCK_ENABLED=true \
  java -jar "$jar_file" > "$LOG_DIR/reviewer.log" 2>&1 &

pid=$!
echo "$pid" > "$PID_FILE"
unset DB_PASSWORD

for _ in $(seq 1 60); do
  code="$(curl -sS -o /tmp/reviewer-health-body.json -w '%{http_code}' --max-time 3 http://localhost:8081/api/repos 2>/dev/null || true)"
  if [[ "$code" =~ ^2 ]]; then
    echo "Reviewer 已启动：http://localhost:8081（/api/repos=${code}）"
    exit 0
  fi
  sleep 1
done

echo "Reviewer 启动超时，最近日志：" >&2
tail -n 80 "$LOG_DIR/reviewer.log" >&2 || true
exit 1
