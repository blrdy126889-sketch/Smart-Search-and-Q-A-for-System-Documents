# 制度文档智能检索与问答系统（docqa-system）

前后端分离架构：
- **前端**（本目录）：Vue 3 + Vite + TypeScript + Element Plus + Pinia + ECharts
- **后端**（`docqa-backend/`）：Spring Boot 3 + PostgreSQL 16 + pgvector

## 快速启动

```bash
# 前端
npm install
npm run dev          # http://localhost:5173（/api 代理至 localhost:8080）

# 后端（需 PostgreSQL + pgvector，见 docqa-backend/README.md）
cd docqa-backend && mvn spring-boot:run
```

## 核心能力

| 模块 | 说明 |
|---|---|
| 知识库构建 | TXT/Word/PDF 上传 → 解析 → 切片(500字/重叠80) → 向量化 → 自动摘要 |
| 混合检索 | BM25 关键词 + pgvector 余弦相似度双路并行召回，RRF 融合排序 |
| RAG 问答 | Top-K 切片 → LLM 流式生成（SSE 打字机）→ 答案自动标注 [n] 来源 |
| 文档管理 | 分类目录、版本管理、审核流、收藏订阅、访问统计 |
| 权限 | RBAC 四角色（USER/EDITOR/AUDITOR/ADMIN）+ 分类级数据权限 |
| 可视化 | 高频问题排行榜、文档引用热度、上传趋势（ECharts）、版本 diff |

## 内置账号

| 账号 | 密码 | 角色 |
|---|---|---|
| admin | 123456 | 系统管理员 |
| editor | 123456 | 文档编辑 |
| auditor | 123456 | 审核员 |
| zhangsan | 123456 | 普通用户 |
