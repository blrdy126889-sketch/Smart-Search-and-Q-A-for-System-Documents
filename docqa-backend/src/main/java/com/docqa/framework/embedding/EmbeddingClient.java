package com.docqa.framework.embedding;

import java.util.List;

/**
 * Embedding 客户端接口
 */
public interface EmbeddingClient {

    List<Double> embed(String text);

    List<List<Double>> embedBatch(List<String> texts);

    int dimension();

    default boolean isMock() { return false; }
}
