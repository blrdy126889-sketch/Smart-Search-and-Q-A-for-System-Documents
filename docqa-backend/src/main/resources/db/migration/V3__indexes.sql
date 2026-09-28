-- =============================================================
-- V3: 检索索引（GIN 全文 + 状态部分索引）
-- 注：向量检索采用应用层余弦排序（兼容无 pgvector 环境）；
--     生产环境启用 pgvector 后可将 embedding 列升级 vector 类型并重建 HNSW 索引
-- =============================================================
CREATE INDEX IF NOT EXISTS idx_chunk_tsv ON biz_doc_chunk USING GIN(tsv);
CREATE INDEX IF NOT EXISTS idx_qalog_answering ON biz_qa_log(status) WHERE status = 'ANSWERING';
