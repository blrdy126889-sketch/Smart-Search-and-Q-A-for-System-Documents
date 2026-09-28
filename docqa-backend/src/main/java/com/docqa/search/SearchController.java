package com.docqa.search;

import com.docqa.common.api.R;
import com.docqa.common.util.SecurityUtils;
import com.docqa.document.mapper.BizDocSocialMapper;
import com.docqa.stats.mapper.BizStatsMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 检索接口：混合检索 + 联想建议
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final HybridSearchService searchService;
    private final BizStatsMapper statsMapper;
    private final BizDocSocialMapper socialMapper;

    @GetMapping
    public R<Map<String, Object>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "HYBRID") String mode,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest req) {
        if (size > 50) size = 50;
        Map<String, Object> result = searchService.search(q, mode, categoryId, page, size);
        try {
            socialMapper.insertAccessLog(SecurityUtils.userId(), null, "SEARCH",
                    q.length() > 480 ? q.substring(0, 480) : q, req.getRemoteAddr());
        } catch (Exception ignore) { }
        return R.ok(result);
    }

    @GetMapping("/suggest")
    public R<List<String>> suggest(@RequestParam String q) {
        if (q == null || q.isBlank()) return R.ok(List.of());
        return R.ok(statsMapper.searchSuggest(q.trim()));
    }
}
