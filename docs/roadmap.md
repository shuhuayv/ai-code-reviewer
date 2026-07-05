# 路线图

## 当前阶段（已完成）

- [x] 项目初始化（Spring Boot 4.1.0 + MyBatis-Plus + MySQL + Redis）
- [x] 数据库设计（6 张业务表）
- [x] 仓库信息管理接口（CRUD + 分页）
- [x] Mock 评审任务接口（创建 + 查询 + 问题 + 报告）
- [x] Swagger 文档
- [x] JGit 仓库克隆（clone repos/{repoId}）
- [x] 代码文件扫描（过滤 + 统计 + SHA-256 + 入库）
- [x] 基于扫描代码的规则驱动评审（7 条规则，优先 Java 核心包）
- [x] Markdown 格式评审报告
- [x] 数据库迁移脚本（migrate_db.sh）

## 下一阶段

- [ ] 接入真实大模型 API（评审）
- [ ] 文件内容读取与 AI 评审 Prompt 构建
- [ ] 真实异步评审流程（PENDING → RUNNING → COMPLETED/FAILED）
- [ ] Prompt 模板管理接口
- [ ] 用户认证与权限
- [ ] 前端页面