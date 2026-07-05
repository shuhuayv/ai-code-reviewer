# 演示脚本使用说明

## 快速开始

```bash
# 确保项目已编译并启动
cd ~/Developer/ai-internship-july/ai-code-reviewer
java -jar target/ai-code-reviewer-1.0.0-SNAPSHOT.jar

# 另开终端，运行演示脚本
chmod +x scripts/*.sh
bash scripts/demo_review_flow.sh
```

## 脚本流程

脚本按顺序执行以下步骤：

| 步骤 | 接口 | 说明 |
|------|------|------|
| 1 | `POST /api/repos` | 创建仓库记录 |
| 2 | `POST /api/repos/{id}/clone` | JGit 克隆仓库到 repos/{id} |
| 3 | `POST /api/repos/{id}/scan` | 扫描代码文件并写入 code_file |
| 4 | `POST /api/reviews/tasks` | 基于扫描代码创建评审任务 |
| 5 | `GET /api/reviews/tasks/{id}` | 查询任务详情 |
| 6 | `GET /api/reviews/tasks/{id}/issues` | 查询评审问题列表 |
| 7 | `GET /api/reviews/tasks/{id}/report` | 查询评审报告 |
| 8 | `GET /api/reviews/tasks/{id}/report/markdown` | 导出 Markdown 报告 |

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `BASE_URL` | `http://localhost:8080` | 服务地址 |
| `REPO_ID` | 自动解析或 2 | 仓库 ID |

## 输出

脚本执行完成后，会在以下位置生成 Markdown 报告：

```
reports/generated/review-task-{taskId}.md
```

`reports/generated/` 目录已在 `.gitignore` 中，不会被提交到 Git。

## 注意事项

- 克隆操作需要外网访问 GitHub，如果网络不通，步骤 2 会失败
- 如果克隆失败，可以跳过 clone/scan，直接使用已有 repoId 创建评审
- 脚本中的 `spring-petclinic` 是示例仓库，可替换为任意公开 Java 仓库