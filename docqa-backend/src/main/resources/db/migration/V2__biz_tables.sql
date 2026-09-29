-- =============================================================
-- V2(MySQL): 业务表（全文检索用 ngram FULLTEXT；向量 TEXT 存储+应用层余弦）
-- =============================================================
CREATE TABLE biz_category (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id     BIGINT       NOT NULL DEFAULT 0,
    ancestors     VARCHAR(500) NOT NULL DEFAULT '0',
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50),
    sort_order    INT          NOT NULL DEFAULT 0,
    doc_count     INT          NOT NULL DEFAULT 0,
    status        SMALLINT     NOT NULL DEFAULT 1,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_category_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_category_perm (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    UNIQUE KEY uk_cat_role (category_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_document (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    audited_at         DATETIME,
    audit_remark       VARCHAR(500),
    deleted_at         DATETIME,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doc_code (doc_code),
    KEY idx_doc_category (category_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_doc_version (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    published_at    DATETIME,
    retry_count     INT          NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doc_version (doc_id, version_no),
    KEY idx_version_doc (doc_id),
    KEY idx_version_hash (file_hash),
    KEY idx_version_pending (index_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_doc_chunk (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id       BIGINT      NOT NULL,
    version_id   BIGINT      NOT NULL,
    chunk_index  INT         NOT NULL,
    content      TEXT        NOT NULL,
    heading_path VARCHAR(500),
    page_no      INT,
    char_count   INT         NOT NULL DEFAULT 0,
    embedding    TEXT        COMMENT '语义向量文本存储 [0.1,0.2,...]，应用层余弦排序',
    is_active    TINYINT     NOT NULL DEFAULT 1,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_version_chunk (version_id, chunk_index),
    KEY idx_chunk_doc (doc_id, is_active),
    FULLTEXT KEY ft_chunk_content (content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_favorite (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT   NOT NULL,
    doc_id     BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_doc (user_id, doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_subscription (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    sub_type   VARCHAR(10) NOT NULL,
    target_id  BIGINT      NOT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sub (user_id, sub_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_notify (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(200) NOT NULL,
    content    VARCHAR(500),
    doc_id     BIGINT,
    is_read    TINYINT      NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notify_user (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_access_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT,
    doc_id     BIGINT,
    action     VARCHAR(20) NOT NULL,
    query_text VARCHAR(500),
    ip         VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_access_doc (doc_id),
    KEY idx_access_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_qa_log (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    sources           JSON,
    feedback          SMALLINT    NOT NULL DEFAULT 0,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_qalog_session (session_id),
    KEY idx_qalog_user (user_id, created_at),
    KEY idx_qalog_answering (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_qa_session (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(40) NOT NULL,
    user_id    BIGINT      NOT NULL,
    title      VARCHAR(200),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_qa_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_stats_daily (
    stat_date     DATE PRIMARY KEY,
    qa_count      INT NOT NULL DEFAULT 0,
    search_count  INT NOT NULL DEFAULT 0,
    hot_keywords  JSON,
    doc_quotes    JSON,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始分类
INSERT INTO biz_category (id, parent_id, ancestors, category_name, sort_order) VALUES
 (1, 0, '0',   '人事制度',   1),
 (2, 0, '0',   '财务制度',   2),
 (3, 0, '0',   '行政制度',   3),
 (4, 1, '0,1', '考勤与假期', 11),
 (5, 1, '0,1', '招聘与入离职', 12),
 (6, 2, '0,2', '报销管理',   21),
 (7, 2, '0,2', '采购管理',   22),
 (8, 3, '0,3', '印章与证照', 31);

INSERT INTO biz_category_perm (category_id, role_id)
SELECT c.id, r.id FROM biz_category c CROSS JOIN sys_role r;
