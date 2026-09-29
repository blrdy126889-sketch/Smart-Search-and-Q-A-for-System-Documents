package com.docqa.config;

import com.docqa.framework.storage.StorageClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储 Bean
 */
@Configuration
public class StorageConfig {

    @Bean
    public StorageClient storageClient(DocQaProperties properties) {
        return new StorageClient(properties.getStorage().getBaseDir());
    }

    /** Chroma 向量库客户端（服务不可用时 available=false，检索自动回退） */
    @Bean
    public com.docqa.framework.vector.ChromaVectorStore chromaVectorStore(DocQaProperties properties) {
        return new com.docqa.framework.vector.ChromaVectorStore(
                properties.getVectorStore().getChromaBaseUrl(),
                properties.getVectorStore().getChromaCollection());
    }
}
