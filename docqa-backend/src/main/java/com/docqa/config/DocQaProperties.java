package com.docqa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "docqa")
public class DocQaProperties {

    private Storage storage = new Storage();
    private Ingest ingest = new Ingest();
    private Search search = new Search();
    private Embedding embedding = new Embedding();
    private Llm llm = new Llm();

    @Data
    public static class Storage {
        private String baseDir;
    }

    @Data
    public static class Ingest {
        private int chunkSize = 500;
        private int chunkOverlap = 80;
        private int embedBatchSize = 32;
        private int retryTimes = 3;
        private long retryIntervalMs = 1000;
    }

    @Data
    public static class Search {
        private int recallSize = 50;
        private double bm25Weight = 0.4;
        private double vectorWeight = 0.6;
        private int rrfK = 60;
        private int qaTopK = 6;
    }

    @Data
    public static class Embedding {
        private String baseUrl;
        private String apiKey;
        private String model;
        private int dimension = 1024;
    }

    @Data
    public static class Llm {
        private String baseUrl;
        private String apiKey;
        private String model;
        private double temperature = 0.3;
        private int timeoutSeconds = 90;
    }
}
