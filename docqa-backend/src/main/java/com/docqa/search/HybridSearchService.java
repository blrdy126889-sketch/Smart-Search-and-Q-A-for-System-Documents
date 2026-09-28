package com.docqa.search;

import com.docqa.common.util.SecurityUtils;
import com.docqa.config.DocQaProperties;
import com.docqa.document.mapper.BizDocChunkMapper;
import com.docqa.framework.embedding.EmbeddingClient;
import com.docqa.framework.tokenizer.Tokenizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 混合检索：BM25（ts_rank_cd）+ 语义（应用层余弦）双路并行召回 → RRF 融合 → 归一化 → 高亮分页
 * 向量兼容设计：embedding 以文本存储，候选集 SQL 同构过滤后由应用层计算余弦排序，
 * 无 pgvector 扩展环境（演示/预览）与生产环境行为一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final BizDocChunkMapper chunkMapper;
    private final EmbeddingClient embeddingClient;
    private final DocQaProperties properties;

    @Autowired
    @Qualifier("searchExecutor")
    private Executor searchExecutor;

    public Map<String, Object> search(String q, String mode, Long categoryId, long page, long size) {
        return search(q, mode, categoryId, page, size, null);
    }

    /** roleIds 显式传入（异步线程无 Sa-Token 上下文时由调用方在请求线程取好） */
    public Map<String, Object> search(String q, String mode, Long categoryId, long page, long size,
                                      List<Long> callerRoleIds) {
        long start = System.currentTimeMillis();
        String trimmed = sanitize(q);
        Map<String, Object> result = new HashMap<>();
        result.put("page", page);
        result.put("size", size);
        if (trimmed.isEmpty()) {
            result.put("records", List.of());
            result.put("total", 0);
            result.put("tookMs", 0);
            return result;
        }
        List<Long> fromCtx = callerRoleIds != null ? callerRoleIds : safeRoleIds();
        final List<Long> roleIds = fromCtx.isEmpty() ? List.of(4L) : fromCtx;

        String tsQuery = Tokenizer.sanitizeForTsQuery(Tokenizer.tokenize(trimmed));
        boolean useKeyword = !"SEMANTIC".equalsIgnoreCase(mode) && !tsQuery.isBlank();
        boolean useVector = !"KEYWORD".equalsIgnoreCase(mode);

        CompletableFuture<List<Map<String, Object>>> bm25Future = useKeyword
                ? CompletableFuture.supplyAsync(() ->
                        chunkMapper.searchByKeyword(tsQuery, categoryId, roleIds, properties.getSearch().getRecallSize()), searchExecutor)
                .orTimeout(2, TimeUnit.SECONDS).exceptionally(e -> {
                    log.warn("BM25 召回失败: {}", e.getMessage());
                    return List.of();
                })
                : CompletableFuture.completedFuture(List.of());

        CompletableFuture<List<Map<String, Object>>> vecFuture = useVector
                ? CompletableFuture.supplyAsync(() -> vectorSearch(trimmed, categoryId, roleIds), searchExecutor)
                .orTimeout(8, TimeUnit.SECONDS).exceptionally(e -> {
                    log.warn("向量召回失败: {}", e.getMessage());
                    return List.of();
                })
                : CompletableFuture.completedFuture(List.of());

        List<Map<String, Object>> fused = rrfFuse(bm25Future.join(), vecFuture.join());
        result.putAll(buildPage(fused, tsQuery, page, size, System.currentTimeMillis() - start));
        return result;
    }

    /** 向量路：查询向量化 → SQL 拉同构过滤候选 → 应用层余弦排序取 TopN */
    private List<Map<String, Object>> vectorSearch(String query, Long categoryId, List<Long> roleIds) {
        List<Double> qVec = embeddingClient.embed(query);
        int candidateSize = properties.getSearch().getRecallSize() * 5;
        List<Map<String, Object>> candidates =
                chunkMapper.searchByVectorCandidates(categoryId, roleIds, candidateSize);
        if (candidates.isEmpty()) return List.of();

        double[] q = toPrimitive(qVec);
        double qNorm = norm(q);
        if (qNorm < 1e-9) return List.of();

        for (Map<String, Object> row : candidates) {
            double[] v = parseEmbedding(String.valueOf(row.get("embedding_text")));
            double cos = cosine(q, qNorm, v);
            row.put("rank", cos);
            row.remove("embedding_text");
        }
        candidates.sort((a, b) -> Double.compare((Double) b.get("rank"), (Double) a.get("rank")));
        return candidates.size() > properties.getSearch().getRecallSize()
                ? new ArrayList<>(candidates.subList(0, properties.getSearch().getRecallSize()))
                : candidates;
    }

    private double[] parseEmbedding(String text) {
        if (text == null || text.length() < 2) return new double[0];
        String s = text.charAt(0) == '[' ? text.substring(1, text.length() - 1) : text;
        String[] parts = s.split(",");
        double[] v = new double[parts.length];
        for (int i = 0; i < parts.length; i++) v[i] = Double.parseDouble(parts[i].trim());
        return v;
    }

    private double cosine(double[] q, double qNorm, double[] v) {
        if (v.length == 0 || v.length != q.length) return 0;
        double dot = 0, vn = 0;
        for (int i = 0; i < q.length; i++) {
            dot += q[i] * v[i];
            vn += v[i] * v[i];
        }
        vn = Math.sqrt(vn);
        return vn < 1e-9 ? 0 : dot / (qNorm * vn);
    }

    private double[] toPrimitive(List<Double> list) {
        double[] a = new double[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = list.get(i);
        return a;
    }

    private double norm(double[] v) {
        double n = 0;
        for (double x : v) n += x * x;
        return Math.sqrt(n);
    }

    private List<Map<String, Object>> rrfFuse(List<Map<String, Object>> bm25Hits, List<Map<String, Object>> vecHits) {
        DocQaProperties.Search cfg = properties.getSearch();
        Map<Long, Map<String, Object>> byChunk = new LinkedHashMap<>();
        accumulate(byChunk, bm25Hits, cfg.getBm25Weight(), "BM25");
        accumulate(byChunk, vecHits, cfg.getVectorWeight(), "VECTOR");
        List<Map<String, Object>> fused = new ArrayList<>(byChunk.values());
        fused.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        return fused;
    }

    @SuppressWarnings("unchecked")
    private void accumulate(Map<Long, Map<String, Object>> byChunk, List<Map<String, Object>> hits,
                            double weight, String tag) {
        int k = properties.getSearch().getRrfK();
        for (int rank = 0; rank < hits.size(); rank++) {
            Map<String, Object> hit = hits.get(rank);
            Long chunkId = ((Number) hit.get("chunk_id")).longValue();
            Map<String, Object> target = byChunk.computeIfAbsent(chunkId, id -> {
                Map<String, Object> m = new HashMap<>(hit);
                m.put("score", 0.0);
                m.put("hitTypes", new ArrayList<String>());
                return m;
            });
            target.put("score", (Double) target.get("score") + weight / (k + rank + 1));
            ((List<String>) target.get("hitTypes")).add(tag);
        }
    }

    private Map<String, Object> buildPage(List<Map<String, Object>> fused, String tsQuery,
                                          long page, long size, long tookMs) {
        double maxScore = fused.isEmpty() ? 1.0 : (Double) fused.get(0).get("score");
        if (maxScore <= 0) maxScore = 1.0;

        int from = (int) Math.min((page - 1) * size, fused.size());
        int to = (int) Math.min(from + size, fused.size());
        List<Map<String, Object>> records = new ArrayList<>();
        for (Map<String, Object> hit : fused.subList(from, to)) {
            String content = String.valueOf(hit.get("content"));
            String snippet = content.length() > 240 ? content.substring(0, 240) + "…" : content;
            if (tsQuery != null && !tsQuery.isBlank()
                    && ((List<?>) hit.get("hitTypes")).contains("BM25")) {
                String hl = chunkMapper.headline(content, tsQuery);
                if (hl != null && !hl.isBlank()) snippet = hl;
            }
            Map<String, Object> vo = new HashMap<>();
            vo.put("docId", ((Number) hit.get("doc_id")).longValue());
            vo.put("docTitle", hit.get("doc_title"));
            vo.put("chunkId", ((Number) hit.get("chunk_id")).longValue());
            vo.put("headingPath", hit.get("heading_path"));
            vo.put("snippet", snippet);
            vo.put("score", (Double) hit.get("score") / maxScore);
            vo.put("hitTypes", hit.get("hitTypes"));
            records.add(vo);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", fused.size());
        result.put("tookMs", tookMs);
        return result;
    }

    /** 异步/非Web线程下安全取角色（无上下文返回空，由调用方兜底） */
    private List<Long> safeRoleIds() {
        try {
            return SecurityUtils.roleIds();
        } catch (Exception e) {
            return List.of();
        }
    }

    private String sanitize(String q) {
        if (q == null) return "";
        String cleaned = q.replaceAll("\\p{Cntrl}", " ").trim();
        return cleaned.length() > 200 ? cleaned.substring(0, 200) : cleaned;
    }
}
