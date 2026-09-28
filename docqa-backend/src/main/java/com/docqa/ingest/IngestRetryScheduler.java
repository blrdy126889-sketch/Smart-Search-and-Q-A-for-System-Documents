package com.docqa.ingest;

import com.docqa.document.entity.BizDocument;
import com.docqa.document.entity.BizDocVersion;
import com.docqa.document.mapper.BizDocVersionMapper;
import com.docqa.document.mapper.BizDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 入库补偿调度：每 5 分钟扫描超时 PENDING/FAILED 版本重入管线（幂等）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestRetryScheduler {

    private final BizDocVersionMapper versionMapper;
    private final BizDocumentMapper documentMapper;
    private final IngestPipeline pipeline;

    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void retryStuckVersions() {
        List<BizDocVersion> candidates = versionMapper.selectRetryCandidates();
        if (candidates.isEmpty()) return;
        log.info("补偿扫描：{} 个版本待重试", candidates.size());
        for (BizDocVersion version : candidates) {
            try {
                BizDocument doc = documentMapper.selectById(version.getDocId());
                if (doc == null) continue;
                versionMapper.casIndexStatus(version.getId(), version.getIndexStatus(), "PENDING", null, 0);
                version.setIndexStatus("PENDING");
                pipeline.ingestAsync(version, doc.getSourceType());
            } catch (Exception e) {
                log.warn("补偿重试失败 versionId={}: {}", version.getId(), e.getMessage());
            }
        }
    }
}
