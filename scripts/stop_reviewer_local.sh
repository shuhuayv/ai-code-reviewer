#!/bin/bash
set -u
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PID_FILE="$PROJECT_DIR/.demo-run/reviewer.pid"

if [[ -f "$PID_FILE" ]]; then
  pid="$(cat "$PID_FILE")"
  command_line="$(ps -p "$pid" -o command= 2>/dev/null || true)"
  if [[ "$command_line" == *"ai-code-reviewer"* ]]; then
    kill "$pid" 2>/dev/null || true
  fi
  rm -f "$PID_FILE"
fi

for pid in $(lsof -ti tcp:8081 2>/dev/null || true); do
  command_line="$(ps -p "$pid" -o command= 2>/dev/null || true)"
  if [[ "$command_line" == *"ai-code-reviewer"* ]] || [[ "$command_line" == *"spring-boot:run"* ]]; then
    kill "$pid" 2>/dev/null || true
  fi
done
echo "Reviewer 已停止"
