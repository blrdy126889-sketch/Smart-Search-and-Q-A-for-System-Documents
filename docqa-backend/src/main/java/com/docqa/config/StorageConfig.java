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
}
