package com.docqa.framework.vector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ChromaDB 向量库客户端（REST v2）：
 * 语义向量独立存储于 Chroma（HNSW cosine），MySQL 仅存元数据与全文索引。
 * 任何 Chroma 异常均向上抛出，由调用方回退 MySQL TEXT 余弦路径。
 */
@Slf4j
public class ChromaVectorStore {

    public record ChromaHit(String chunkId, long docId, double cosine) { }

    private final RestClient client;
    private final String collectionName;
    private volatile String collectionId;
    private volatile boolean available;

    public ChromaVectorStore(String baseUrl, String collectionName) {
        this.collectionName = collectionName;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        try {
            this.client.get().uri("/api/v2/heartbeat").retrieve().toEntity(String.class);
            this.available = true;
        } catch (Exception e) {
            this.available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    private String collectionId() {
        if (collectionId == null) {
            Map<String, Object> body = new HashMap<>();
            body.put("name", collectionName);
            body.put("get_or_create", true);
            // cosine 距离空间（1-distance 即余弦相似度）
            body.put("configuration_json", Map.of("hnsw", Map.of("space", "cosine")));
            Map<?, ?> col = client.post()
                    .uri("/api/v2/tenants/default_tenant/databases/default_database/collections")
                    .body(body).retrieve().body(Map.class);
            collectionId = String.valueOf(col.get("id"));
            log.info("Chroma collection '{}' ready: {}", collectionName, collectionId);
        }
        return collectionId;
    }

    private String base() {
        return "/api/v2/tenants/default_tenant/databases/default_database/collections/" + collectionId();
    }

    /** 批量写入切片向量（ids 与 embeddings 一一对应） */
    public void upsertBatch(List<String> ids, List<List<Double>> embeddings,
                            List<String> documents, List<Map<String, Object>> metadatas) {
        if (ids == null || ids.isEmpty()) return;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ids", ids);
        body.put("embeddings", embeddings);
        body.put("documents", documents);
        body.put("metadatas", metadatas);
        client.post().uri(base() + "/upsert").body(body).retrieve().toEntity(String.class);
    }

    /** 按文档维度删除全部切片向量（版本切换/下线/删除文档时） */
    public void deleteByDocId(long docId) {
        Map<String, Object> body = Map.of("where", Map.of("doc_id", docId));
        client.post().uri(base() + "/delete").body(body).retrieve().toEntity(String.class);
    }

    /**
     * TopK 相似检索（限定可见文档集，权限同构）
     * @return 余弦相似度降序命中
     */
    public List<ChromaHit> queryTopK(List<Double> queryVector, List<Long> visibleDocIds, int topK) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query_embeddings", List.of(queryVector));
        body.put("n_results", Math.min(topK, Math.max(1, visibleDocIds.size())));
        body.put("where", Map.of("doc_id", Map.of("$in", visibleDocIds)));
        body.put("include", List.of("metadatas", "distances"));
        Map<?, ?> resp = client.post().uri(base() + "/query").body(body).retrieve().body(Map.class);
        List<ChromaHit> hits = new ArrayList<>();
        if (resp == null) return hits;
        List<?> ids = (List<?>) ((List<?>) resp.get("ids")).get(0);
        List<?> dists = (List<?>) ((List<?>) resp.get("distances")).get(0);
        List<?> metas = (List<?>) ((List<?>) resp.get("metadatas")).get(0);
        for (int i = 0; i < ids.size(); i++) {
            Map<?, ?> meta = (Map<?, ?>) metas.get(i);
            double dist = ((Number) dists.get(i)).doubleValue();
            hits.add(new ChromaHit(String.valueOf(ids.get(i)),
                    ((Number) meta.get("doc_id")).longValue(), 1.0 - dist));
        }
        return hits;
    }
}
