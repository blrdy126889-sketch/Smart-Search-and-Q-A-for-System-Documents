package com.docqa.framework.embedding;

import com.docqa.config.DocQaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock 向量客户端：未配置 API Key 时启用。
 * 词袋哈希确定性伪向量（同词同向量）+ L2 归一化，保证演示环境语义检索链路可用。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${docqa.embedding.api-key:}' == ''")
public class MockEmbeddingClient implements EmbeddingClient {

    private final DocQaProperties properties;
    private final Map<String, double[]> wordVecCache = new ConcurrentHashMap<>();

    @Override
    public List<Double> embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        List<List<Double>> result = new ArrayList<>(texts.size());
        for (String text : texts) {
            result.add(vectorize(text));
        }
        return result;
    }

    private List<Double> vectorize(String text) {
        int dim = dimension();
        double[] vec = new double[dim];
        List<String> tokens = new ArrayList<>(List.of(text.split("[\\s，。；、！？,.!?;:：\\n（）()\\[\\]【】\"'']+")));
        if (tokens.isEmpty()) tokens.add("_empty_");
        List<String> finalTokens = new ArrayList<>(tokens);
        for (String t : tokens) {
            for (int i = 0; i + 2 <= t.length(); i++) {
                finalTokens.add(t.substring(i, i + 2));
            }
        }
        for (String token : finalTokens) {
            double[] wv = wordVecCache.computeIfAbsent(token, k -> {
                double[] v = new double[dim];
                java.util.Random r = new java.util.Random(hash(k));
                for (int i = 0; i < dim; i++) v[i] = r.nextGaussian();
                return v;
            });
            for (int i = 0; i < dim; i++) vec[i] += wv[i];
        }
        double norm = 0;
        for (double v : vec) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm < 1e-9) norm = 1.0;
        List<Double> out = new ArrayList<>(dim);
        for (double v : vec) out.add(v / norm);
        return out;
    }

    private long hash(String s) {
        long h = 1125899906842597L;
        for (int i = 0; i < s.length(); i++) h = 31 * h + s.charAt(i);
        return h;
    }

    @Override
    public int dimension() {
        return properties.getEmbedding().getDimension();
    }

    @Override
    public boolean isMock() { return true; }
}
