-- =============================================================
-- V2: 业务表（分类/文档/版本/切片/收藏/订阅/通知/访问日志/问答日志/统计）
-- =============================================================

-- 分类目录（树形：parent_id + ancestors 物化路径）
CREATE TABLE biz_category (
    id           BIGSERIAL PRIMARY KEY,
    parent_id    BIGINT       NOT NULL DEFAULT 0,
    ancestors    VARCHAR(500) NOT NULL DEFAULT '0',
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50),
    sort_order   INT          NOT NULL DEFAULT 0,
    doc_count    INT          NOT NULL DEFAULT 0,
    status       SMALLINT     NOT NULL DEFAULT 1,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_category_parent ON biz_category(parent_id);

-- 分类-角色数据授权（检索可见范围）
CREATE TABLE biz_category_perm (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    UNIQUE (category_id, role_id)
);

-- 制度文档主表
CREATE TABLE biz_document (
    id                 BIGSERIAL PRIMARY KEY,
    doc_code           VARCHAR(64)  NOT NULL,
    title              VARCHAR(200) NOT NULL,
    category_id        BIGINT       NOT NULL,
    current_version_id BIGINT,
    status             VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    summary            TEXT,
    source_type        VARCHAR(10)  NOT NULL,
    file_size          BIGINT       NOT NULL DEFAULT 0,
    secret_level       SMALLINT     NOT NULL DEFAULT 1,
    effective_date     DATE,
    view_count         INT          NOT NULL DEFAULT 0,
    quote_count        INT          NOT NULL DEFAULT 0,
    favorite_count     INT          NOT NULL DEFAULT 0,
    owner_id           BIGINT,
    audited_by         BIGINT,
    audited_at         TIMESTAMPTZ,
    audit_remark       VARCHAR(500),
    deleted_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_doc_code ON biz_document(doc_code) WHERE deleted_at IS NULL;
CREATE INDEX idx_doc_category ON biz_document(category_id, status);

-- 文档版本表（入库状态机所在）
CREATE TABLE biz_doc_version (
    id              BIGSERIAL PRIMARY KEY,
    doc_id          BIGINT       NOT NULL,
    version_no      VARCHAR(20)  NOT NULL,
    title_snapshot  VARCHAR(200) NOT NULL,
    file_path       VARCHAR(500) NOT NULL,
    file_hash       CHAR(64)     NOT NULL,
    plain_text_path VARCHAR(500),
    char_count      INT          NOT NULL DEFAULT 0,
    chunk_count     INT          NOT NULL DEFAULT 0,
    index_status    VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    fail_reason     TEXT,
    summary         TEXT,
    change_log      VARCHAR(1000),
    created_by      BIGINT,
    published_at    TIMESTAMPTZ,
    retry_count     INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (doc_id, version_no)
);
CREATE INDEX idx_version_doc ON biz_doc_version(doc_id);
CREATE INDEX idx_version_hash ON biz_doc_version(file_hash);
CREATE INDEX idx_version_pending ON biz_doc_version(index_status)
  WHERE index_status IN ('PENDING', 'FAILED');

-- 文档切片表（检索核心：tsv 全文 + embedding 向量）
CREATE TABLE biz_doc_chunk (
    id           BIGSERIAL PRIMARY KEY,
    doc_id       BIGINT      NOT NULL,
    version_id   BIGINT      NOT NULL,
    chunk_index  INT         NOT NULL,
    content      TEXT        NOT NULL,
    heading_path VARCHAR(500),
    page_no      INT,
    char_count   INT         NOT NULL DEFAULT 0,
    -- 向量列：优先 vector(1024)（有 pgvector 时）；此处用 TEXT 兼容存储 '[0.1,0.2,...]'，
    -- PgVectorTypeHandler 双向字符串编解码，生产升级 pgvector 后可平滑 ALTER
    embedding    TEXT,
    tsv          tsvector,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (version_id, chunk_index)
);
CREATE INDEX idx_chunk_doc ON biz_doc_chunk(doc_id, is_active);

-- 收藏表
CREATE TABLE biz_favorite (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    doc_id     BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, doc_id)
);

-- 订阅表（分类/文档两类）
CREATE TABLE biz_subscription (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    sub_type   VARCHAR(10) NOT NULL,
    target_id  BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, sub_type, target_id)
);

-- 订阅通知表
CREATE TABLE biz_notify (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(200) NOT NULL,
    content    VARCHAR(500),
    doc_id     BIGINT,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notify_user ON biz_notify(user_id, is_read);

-- 访问日志表（按月 RANGE 分区）
CREATE TABLE biz_access_log (
    id         BIGSERIAL,
    user_id    BIGINT,
    doc_id     BIGINT,
    action     VARCHAR(20) NOT NULL,
    query_text VARCHAR(500),
    ip         VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
CREATE INDEX idx_access_doc ON biz_access_log(doc_id);
CREATE INDEX idx_access_user ON biz_access_log(user_id, created_at);
CREATE TABLE biz_access_log_2026_09 PARTITION OF biz_access_log FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
CREATE TABLE biz_access_log_2026_10 PARTITION OF biz_access_log FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
CREATE TABLE biz_access_log_2026_11 PARTITION OF biz_access_log FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE biz_access_log_2026_12 PARTITION OF biz_access_log FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');
CREATE TABLE biz_access_log_2027_01 PARTITION OF biz_access_log FOR VALUES FROM ('2027-01-01') TO ('2027-02-01');
CREATE TABLE biz_access_log_2027_02 PARTITION OF biz_access_log FOR VALUES FROM ('2027-02-01') TO ('2027-03-01');
CREATE TABLE biz_access_log_default PARTITION OF biz_access_log DEFAULT;

-- 问答日志表
CREATE TABLE biz_qa_log (
    id                BIGSERIAL PRIMARY KEY,
    session_id        VARCHAR(40) NOT NULL,
    user_id           BIGINT      NOT NULL,
    question          TEXT        NOT NULL,
    answer            TEXT,
    model             VARCHAR(50),
    prompt_tokens     INT,
    completion_tokens INT,
    latency_ms        INT,
    first_token_ms    INT,
    status            VARCHAR(20) NOT NULL DEFAULT 'ANSWERING',
    sources           JSONB,
    feedback          SMALLINT    NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_qalog_session ON biz_qa_log(session_id);
CREATE INDEX idx_qalog_user ON biz_qa_log(user_id, created_at);

-- 问答会话表
CREATE TABLE biz_qa_session (
    id         BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(40) NOT NULL,
    user_id    BIGINT      NOT NULL,
    title      VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_qa_session ON biz_qa_session(session_id);

-- 统计汇总日表
CREATE TABLE biz_stats_daily (
    stat_date     DATE PRIMARY KEY,
    qa_count      INT    NOT NULL DEFAULT 0,
    search_count  INT    NOT NULL DEFAULT 0,
    hot_keywords  JSONB,
    doc_quotes    JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 初始分类目录
INSERT INTO biz_category (id, parent_id, ancestors, category_name, sort_order) VALUES
 (1, 0, '0',   '人事制度',   1),
 (2, 0, '0',   '财务制度',   2),
 (3, 0, '0',   '行政制度',   3),
 (4, 1, '0,1', '考勤与假期', 11),
 (5, 1, '0,1', '招聘与入离职', 12),
 (6, 2, '0,2', '报销管理',   21),
 (7, 2, '0,2', '采购管理',   22),
 (8, 3, '0,3', '印章与证照', 31);
SELECT setval('biz_category_id_seq', 8);

INSERT INTO biz_category_perm (category_id, role_id)
SELECT c.id, r.id FROM biz_category c CROSS JOIN sys_role r WHERE r.role_code IN ('USER', 'EDITOR', 'AUDITOR', 'ADMIN');
