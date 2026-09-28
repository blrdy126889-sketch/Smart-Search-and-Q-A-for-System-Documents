package com.docqa.framework.embedding;

import com.docqa.common.exception.BizException;
import com.docqa.config.DocQaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 协议兼容 Embedding HTTP 客户端（智谱 embedding-3 等）
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${docqa.embedding.api-key:}' != ''")
public class HttpEmbeddingClient implements EmbeddingClient {

    private final DocQaProperties properties;
    private final WebClient.Builder webClientBuilder;

    @Override
    public List<Double> embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<List<Double>> embedBatch(List<String> texts) {
        DocQaProperties.Embedding cfg = properties.getEmbedding();
        Map<String, Object> body = new HashMap<>();
        body.put("model", cfg.getModel());
        body.put("input", texts);

        Map<String, Object> resp = webClientBuilder.build().post()
                .uri(cfg.getBaseUrl() + "/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(60));
        if (resp == null || resp.get("data") == null) {
            throw new BizException("Embedding 服务返回空结果");
        }
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.get("data");
        return data.stream()
                .sorted(java.util.Comparator.comparingInt(d -> ((Number) d.get("index")).intValue()))
                .map(d -> {
                    List<Number> vec = (List<Number>) d.get("embedding");
                    return vec.stream().map(Number::doubleValue).toList();
                })
                .toList();
    }

    @Override
    public int dimension() {
        return properties.getEmbedding().getDimension();
    }
}
