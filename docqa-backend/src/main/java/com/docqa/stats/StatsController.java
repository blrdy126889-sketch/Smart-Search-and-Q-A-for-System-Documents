package com.docqa.stats;

import com.docqa.common.api.R;
import com.docqa.stats.mapper.BizStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能数据统计分析
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final BizStatsMapper statsMapper;

    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> docStat = statsMapper.docOverview();
        data.put("docCount", toLong(docStat.get("doc_count")));
        data.put("publishedCount", toLong(docStat.get("published_count")));
        data.put("pendingAuditCount", toLong(docStat.get("pending_audit_count")));
        data.put("todayQaCount", statsMapper.todayQaCount());
        data.put("failedIndexCount", statsMapper.failedIndexCount());
        return R.ok(data);
    }

    @GetMapping("/hot-questions")
    public R<List<Map<String, Object>>> hotQuestions(@RequestParam(defaultValue = "30") int days,
                                                     @RequestParam(defaultValue = "10") int topN) {
        if (days < 1 || days > 365) days = 30;
        if (topN < 1 || topN > 50) topN = 10;
        return R.ok(statsMapper.hotQuestions(days, topN));
    }

    @GetMapping("/doc-quotes")
    public R<List<Map<String, Object>>> docQuotes(@RequestParam(defaultValue = "30") int days,
                                                  @RequestParam(defaultValue = "10") int topN) {
        if (days < 1 || days > 365) days = 30;
        if (topN < 1 || topN > 50) topN = 10;
        try {
            List<Map<String, Object>> result = statsMapper.docQuotes(days, topN);
            if (!result.isEmpty()) return R.ok(result);
        } catch (Exception e) {
            log.warn("引用热度统计失败，走兜底: {}", e.getMessage());
        }
        return R.ok(statsMapper.docQuotesFallback(topN));
    }

    @GetMapping("/upload-trend")
    public R<List<Map<String, Object>>> uploadTrend(@RequestParam(defaultValue = "6") int months) {
        if (months < 1 || months > 24) months = 6;
        return R.ok(statsMapper.uploadTrend(months));
    }

    private long toLong(Object v) {
        return v == null ? 0 : ((Number) v).longValue();
    }
}
