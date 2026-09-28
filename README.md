# Smart Search & Q&A for System Documents（制度文档智能检索与问答系统）

> 基于 RAG 的企业制度知识中枢：文档全生命周期管理 + BM25/语义双路混合检索 + 流式智能问答（答案自动标注来源）

![Vue](https://img.shields.io/badge/前端-Vue%203-42b883) ![SpringBoot](https://img.shields.io/badge/后端-Spring%20Boot%203-6db33f) ![PostgreSQL](https://img.shields.io/badge/数据库-PostgreSQL%2016-336791) ![RAG](https://img.shields.io/badge/架构-RAG-ff6f00) ![License](https://img.shields.io/badge/license-MIT-blue)

## ✨ 功能特性

### 三大核心场景

| 场景 | 能力 |
|---|---|
| 📚 **知识库构建** | TXT/Word/PDF 上传 → 解析（PDFBox 按页/Tika 按节）→ 递归切片（500字/重叠80字）→ 批量向量化 → LLM 自动摘要；状态机 + 定时补偿保证入库可靠性 |
| 🔎 **混合检索** | BM25 关键词（tsvector+GIN, ts_rank_cd）与 语义向量（余弦相似度）**双路并行召回**，RRF 融合排序，关键词命中高亮，支持混合/关键词/语义三模式切换 |
| 💬 **RAG 智能问答** | Top-K 切片注入上下文 → LLM 流式生成（**SSE 打字机效果**）→ 答案自动标注 `[n]` 来源 → 点击脚注跳转文档原文；无答案时明确声明，拒绝编造 |

### 管理能力

- **制度文档管理**：分类目录（树形+数据授权）、版本管理（diff 双栏对比）、审核流（提交→通过/驳回→发布/下线）、收藏订阅通知、访问统计
- **权限体系**：RBAC 四角色（USER/EDITOR/AUDITOR/ADMIN）+ 分类级数据权限（检索 SQL 行级过滤，双路同构保证权限语义一致）
- **智能分析可视化**：ECharts 高频咨询问题 TOP 榜、文档被引用热度、上传趋势；问答/操作/访问三类日志
- **工程化**：Flyway 自动迁移 13 表、AOP 操作日志、Swagger 文档、docker-compose 一键部署

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────┐
│  前端 Vue3 + Vite + TS + Element Plus + Pinia + ECharts │
│  用户端(portal): 检索/问答(SSE打字机)/文档中心/我的       │
│  管理端(admin):  仪表盘/文档/分类/审核/统计/用户/日志     │
└────────────────────────┬────────────────────────────────┘
                         │ RESTful /api/v1 (JWT via Sa-Token)
┌────────────────────────▼────────────────────────────────┐
│  后端 Spring Boot 3 (Java 17) + MyBatis-Plus             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐  │
│  │ 入库管线  │ │ 混合检索  │ │ RAG问答   │ │ RBAC+数据  │  │
│  │ 解析→切片 │ │ BM25+余弦│ │ SSE推流  │ │ 权限+日志  │  │
│  │ →向量→摘要│ │ RRF融合  │ │ 来源标注 │ │ AOP审计    │  │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘  │
│  模型抽象层: EmbeddingClient / LlmClient (OpenAI兼容协议) │
│  未配置 API Key 时自动降级 Mock，全链路零依赖可运行        │
└──────┬──────────────────────────────────┬───────────────┘
       │                                  │
┌──────▼───────────┐          ┌───────────▼───────────────┐
│ PostgreSQL 16    │          │ 模型服务(可选)             │
│ 关系数据 + 13表   │          │ 智谱 embedding-3 / GLM    │
│ tsv全文+余弦向量  │          │ 或任意OpenAI兼容端点       │
│ (Flyway自动迁移)  │          │ 或本地 vLLM/Ollama        │
└──────────────────┘          └───────────────────────────┘
```

## 🚀 快速开始

### 方式一：Docker Compose 一键启动（推荐）

```bash
git clone https://github.com/blrdy126889-sketch/Smart-Search-and-Q-A-for-System-Documents.git
cd Smart-Search-and-Q-A-for-System-Documents/docqa-backend
docker compose up -d        # pgvector + backend + frontend
# 前端: http://localhost:5173   后端: http://localhost:8080
```

### 方式二：本地开发

```bash
# 1. 数据库（任意 PG16 实例，或 docker run pgvector/pgvector:pg16）
createdb docqa

# 2. 后端（Flyway 自动建表 + 内置账号）
cd docqa-backend && mvn spring-boot:run

# 3. 前端
npm install && npm run dev   # vite 代理 /api → localhost:8080
```

### 内置账号

| 账号 | 密码 | 角色 | 权限 |
|---|---|---|---|
| admin | 123456 | 系统管理员 | 全部功能 |
| editor | 123456 | 文档编辑 | 上传/编辑/分类 |
| auditor | 123456 | 审核员 | 审核/下线/日志 |
| zhangsan | 123456 | 普通用户 | 检索/问答/收藏/订阅 |

## ⚙️ 配置（环境变量）

| 变量 | 默认 | 说明 |
|---|---|---|
| DOCQA_DB_HOST / PORT / NAME / USER / PASSWORD | localhost/5432/docqa/docqa/docqa123 | PostgreSQL 连接 |
| LLM_API_KEY | 空 | 大模型 Key；**为空自动降级 Mock 模式**（检索/流式问答链路照常可演示） |
| LLM_BASE_URL / LLM_MODEL | 智谱 / glm-4-flash | 任意 OpenAI 兼容端点 |
| EMBEDDING_API_KEY / EMBEDDING_BASE_URL / EMBEDDING_MODEL | 空 / 智谱 / embedding-3 | 向量服务，同为 OpenAI 兼容协议 |

## 📖 API 一览（前缀 /api/v1，Swagger: /swagger-ui.html）

```
POST /auth/login                登录（JWT）
GET  /search?q=&mode=HYBRID     混合检索（HYBRID/KEYWORD/SEMANTIC）
POST /documents/upload          上传文档（multipart，TXT/Word/PDF ≤50MB）
POST /documents/{id}/audit      审核通过/驳回
POST /qa/stream                 RAG 流式问答（SSE: meta→delta→done）
GET  /stats/hot-questions       高频咨询问题 TOP
GET  /stats/doc-quotes          文档引用热度 TOP
... 共 40+ RESTful 接口
```

## 🗂️ 项目结构

```
├── src/                        # 前端 Vue3（16 页面）
│   ├── views/portal/           #   用户端：检索/问答(Chat)/文档/我的
│   ├── views/admin/            #   管理端：仪表盘/审核/统计/权限
│   └── composables/useSseChat.ts  # SSE 打字机核心
├── docqa-backend/              # 后端 Spring Boot 3
│   └── src/main/java/com/docqa/
│       ├── framework/          #   parser/chunker/embedding/llm/tokenizer 抽象
│       ├── ingest/             #   入库管线（状态机+补偿调度）
│       ├── search/             #   混合检索（双路并行+RRF）
│       ├── qa/                 #   RAG SSE 流式问答
│       └── document|system|category|stats/
│   └── src/main/resources/db/migration/   # Flyway V1-V3（13表）
└── docqa-backend/docker-compose.yml
```

## 🔬 关键设计

- **向量存储自适应**：优先 pgvector `vector(1024)`；无扩展环境自动降级 TEXT 存储 + 应用层余弦排序，行为一致（生产可平滑升级）
- **入库可靠性**：`PENDING→PARSING→CHUNKING→EMBEDDING→READY/FAILED` 状态机 + 乐观锁迁移 + 每 5 分钟补偿重试（幂等）
- **版本瞬切**：发布新版本时旧切片 `is_active=false`，检索立即切换无脏数据
- **六大短事务**：上传/切片/向量化/发布/问答收尾/删除，替代长事务
- **性能基线**：混合检索 P95 ≤ 800ms｜问答首 token ≤ 3s｜5MB PDF 入库 ≤ 60s

## 📄 License

MIT
