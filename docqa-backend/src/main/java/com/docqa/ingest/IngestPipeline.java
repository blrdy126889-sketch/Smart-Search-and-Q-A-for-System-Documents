package com.docqa.ingest;

import com.docqa.category.mapper.BizCategoryMapper;
import com.docqa.common.exception.BizException;
import com.docqa.config.DocQaProperties;
import com.docqa.document.entity.BizDocChunk;
import com.docqa.document.entity.BizDocVersion;
import com.docqa.document.mapper.BizDocChunkMapper;
import com.docqa.document.mapper.BizDocVersionMapper;
import com.docqa.framework.chunker.Chunker;
import com.docqa.framework.embedding.EmbeddingClient;
import com.docqa.framework.llm.LlmClient;
import com.docqa.framework.llm.LlmClient.ChatMessage;
import com.docqa.framework.parser.DocumentParser.ParsedDocument;
import com.docqa.framework.parser.ParserFactory;
import com.docqa.framework.storage.StorageClient;
import com.docqa.framework.tokenizer.Tokenizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档入库管线：解析 → 切片 → 向量化 → 就绪（状态机 + 乐观锁 + 批量重试 + 自动摘要）
 * 状态机：PENDING → PARSING → CHUNKING → EMBEDDING → READY / FAILED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestPipeline {

    private final ParserFactory parserFactory;
    private final StorageClient storageClient;
    private final EmbeddingClient embeddingClient;
    private final com.docqa.framework.vector.ChromaVectorStore chroma;
    private final LlmClient llmClient;
    private final BizDocVersionMapper versionMapper;
    private final BizDocChunkMapper chunkMapper;
    private final BizCategoryMapper categoryMapper;
    private final DocQaProperties properties;

    @Async("ingestExecutor")
    public void ingestAsync(BizDocVersion version, String sourceType) {
        try {
            ingest(version, sourceType);
        } catch (Exception e) {
            log.error("入库管线异常 versionId={}: {}", version.getId(), e.getMessage(), e);
        }
    }

    public void ingest(BizDocVersion version, String sourceType) {
        Long versionId = version.getId();
        // 兜底：等待上传主事务提交（异步触发可能早于 commit）
        for (int i = 0; i < 20 && versionMapper.selectById(versionId) == null; i++) {
            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }

        // ① PARSING
        if (!cas(versionId, "PENDING", "PARSING") && !"PARSING".equals(currentStatus(versionId))) {
            return;
        }
        ParsedDocument parsed;
        try {
            parsed = parserFactory.parse(sourceType, storageClient.read(version.getFilePath()));
        } catch (Exception e) {
            fail(versionId, "解析失败：" + e.getMessage());
            return;
        }
        String plainTextPath = storageClient.saveText(parsed.fullText());

        // ② CHUNKING（幂等：先清旧 chunk）
        cas(versionId, "PARSING", "CHUNKING");
        Chunker chunker = new Chunker(properties.getIngest().getChunkSize(), properties.getIngest().getChunkOverlap());
        List<Chunker.TextChunk> textChunks;
        try {
            textChunks = chunker.chunk(parsed);
        } catch (Exception e) {
            fail(versionId, "切片失败：" + e.getMessage());
            return;
        }
        List<BizDocChunk> chunks = new ArrayList<>();
        for (int i = 0; i < textChunks.size(); i++) {
            Chunker.TextChunk tc = textChunks.get(i);
            BizDocChunk chunk = new BizDocChunk();
            chunk.setDocId(version.getDocId());
            chunk.setVersionId(versionId);
            chunk.setChunkIndex(i);
            chunk.setContent(tc.content());
            chunk.setHeadingPath(tc.headingPath());
            chunk.setPageNo(tc.pageNo());
            chunk.setCharCount(tc.content() == null ? 0 : tc.content().length());
            chunk.setIsActive(true);
            chunk.setTsvText(Tokenizer.tokenize((tc.headingPath() == null ? "" : tc.headingPath() + " ") + tc.content()));
            chunks.add(chunk);
        }
        chunkMapper.deleteByVersionId(versionId);
        for (int i = 0; i < chunks.size(); i += 100) {
            chunkMapper.batchInsertWithTsv(chunks.subList(i, Math.min(chunks.size(), i + 100)));
        }

        // ③ EMBEDDING（外部调用不在事务内；批次重试）
        cas(versionId, "CHUNKING", "EMBEDDING");
        int batchSize = properties.getIngest().getEmbedBatchSize();
        List<Long> chunkIds = chunks.stream().map(BizDocChunk::getId).toList();
        List<String> contents = chunks.stream().map(BizDocChunk::getContent).toList();
        for (int i = 0; i < contents.size(); i += batchSize) {
            List<String> batch = contents.subList(i, Math.min(contents.size(), i + batchSize));
            List<List<Double>> vectors = embedWithRetry(batch);
            for (int j = 0; j < vectors.size(); j++) {
                updateChunkEmbedding(chunkIds.get(i + j), vectors.get(j));
            }
            // 双写：向量同步至 ChromaDB（失败不阻断入库，检索自动回退 MySQL TEXT）
            upsertChroma(chunkIds.subList(i, i + vectors.size()), vectors, batch,
                    chunks.subList(i, i + vectors.size()));
        }

        // ④ READY
        versionMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<BizDocVersion>()
                .eq(BizDocVersion::getId, versionId)
                .set(BizDocVersion::getPlainTextPath, plainTextPath)
                .set(BizDocVersion::getCharCount, parsed.fullText() == null ? 0 : parsed.fullText().length())
                .set(BizDocVersion::getChunkCount, chunks.size())
                .set(BizDocVersion::getIndexStatus, "READY")
                .set(BizDocVersion::getUpdatedAt, OffsetDateTime.now()));
        log.info("入库完成 versionId={}, docId={}, chunks={}", versionId, version.getDocId(), chunks.size());

        // ⑤ 自动摘要（失败不影响就绪）
        try {
            generateSummary(versionId, parsed.fullText());
        } catch (Exception e) {
            log.warn("自动摘要生成失败 versionId={}: {}", versionId, e.getMessage());
        }
        categoryMapper.refreshDocCount(version.getDocId());
    }

    private List<List<Double>> embedWithRetry(List<String> batch) {
        DocQaProperties.Ingest cfg = properties.getIngest();
        BizException last = null;
        for (int attempt = 0; attempt <= cfg.getRetryTimes(); attempt++) {
            try {
                return embeddingClient.embedBatch(batch);
            } catch (Exception e) {
                last = new BizException("向量化失败：" + e.getMessage());
                log.warn("向量化批次失败（第{}次）：{}", attempt + 1, e.getMessage());
                try { Thread.sleep(cfg.getRetryIntervalMs() * (1L << attempt)); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        throw last != null ? last : new BizException("向量化失败");
    }

    private void upsertChroma(List<Long> ids, List<List<Double>> vectors,
                              List<String> contents, List<BizDocChunk> meta) {
        if (!chroma.isAvailable()) return;
        try {
            List<String> cid = ids.stream().map(String::valueOf).toList();
            List<Map<String, Object>> metas = new ArrayList<>();
            for (BizDocChunk c : meta) {
                Map<String, Object> m = new HashMap<>();
                m.put("doc_id", c.getDocId());
                m.put("version_id", c.getVersionId());
                m.put("chunk_index", c.getChunkIndex());
                m.put("heading_path", c.getHeadingPath() == null ? "" : c.getHeadingPath());
                metas.add(m);
            }
            chroma.upsertBatch(cid, vectors, contents, metas);
        } catch (Exception e) {
            log.warn("Chroma 向量写入失败(已回退MySQL TEXT): {}", e.getMessage());
        }
    }

    private void updateChunkEmbedding(Long chunkId, List<Double> vector) {
        String vecText = vector.stream()
                .map(v -> String.format("%.6f", v))
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        chunkMapper.updateChunkEmbeddingText(chunkId, vecText);
    }

    private void generateSummary(Long versionId, String fullText) {
        if (fullText == null || fullText.isBlank()) return;
        String input = fullText.length() > 4000 ? fullText.substring(0, 4000) : fullText;
        String prompt = "请用不超过200字概括以下制度文档的核心内容，输出纯文本摘要：\n\n" + input;
        String summary = llmClient.chat(List.of(ChatMessage.user(prompt)));
        if (summary != null && !summary.isBlank()) {
            versionMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<BizDocVersion>()
                    .eq(BizDocVersion::getId, versionId)
                    .set(BizDocVersion::getSummary, summary.length() > 600 ? summary.substring(0, 600) : summary));
        }
    }

    private boolean cas(Long versionId, String from, String to) {
        return versionMapper.casIndexStatus(versionId, from, to, null, 0) > 0;
    }

    private void fail(Long versionId, String reason) {
        versionMapper.casIndexStatus(versionId, "PARSING", "FAILED", reason, 1);
        versionMapper.casIndexStatus(versionId, "CHUNKING", "FAILED", reason, 1);
        versionMapper.casIndexStatus(versionId, "EMBEDDING", "FAILED", reason, 1);
        log.warn("入库失败 versionId={}: {}", versionId, reason);
    }

    private String currentStatus(Long versionId) {
        return versionMapper.selectById(versionId).getIndexStatus();
    }
}
