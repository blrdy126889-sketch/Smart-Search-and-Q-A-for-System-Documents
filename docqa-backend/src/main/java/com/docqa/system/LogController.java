package com.docqa.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.docqa.common.api.PageResult;
import com.docqa.common.api.R;
import com.docqa.common.util.SecurityUtils;
import com.docqa.document.mapper.BizDocSocialMapper;
import com.docqa.qa.mapper.BizQaMapper;
import com.docqa.system.entity.SysOperationLog;
import com.docqa.system.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日志查询：操作日志 / 问答日志 / 访问日志
 */
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogController {

    private final SysOperationLogMapper opLogMapper;
    private final BizQaMapper qaMapper;
    private final BizDocSocialMapper socialMapper;

    @GetMapping("/operations")
    public R<PageResult<Map<String, Object>>> operations(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "15") long size,
            @RequestParam(required = false) String module) {
        SecurityUtils.checkPerm("system:log:list");
        Page<SysOperationLog> p = opLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysOperationLog>()
                        .eq(module != null && !module.isBlank(), SysOperationLog::getModule, module)
                        .orderByDesc(SysOperationLog::getId));
        List<Map<String, Object>> records = p.getRecords().stream().map(l -> {
            Map<String, Object> m = new HashMap<>();
            m.put("createdAt", l.getCreatedAt());
            m.put("username", l.getUsername());
            m.put("module", l.getModule());
            m.put("operation", l.getOperation());
            m.put("requestMethod", l.getRequestMethod());
            m.put("requestPath", l.getRequestPath());
            m.put("resultCode", l.getResultCode());
            m.put("errorMsg", l.getErrorMsg());
            m.put("costMs", l.getCostMs());
            return m;
        }).toList();
        return R.ok(PageResult.of(records, p.getTotal(), page, size));
    }

    @GetMapping("/qa")
    public R<PageResult<Map<String, Object>>> qaLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "15") long size) {
        SecurityUtils.checkPerm("system:log:list");
        return R.ok(PageResult.of(
                qaMapper.selectQaLogs((page - 1) * size, size),
                qaMapper.countQaLogs(), page, size));
    }

    @GetMapping("/access")
    public R<PageResult<Map<String, Object>>> accessLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "15") long size) {
        SecurityUtils.checkPerm("system:log:list");
        return R.ok(PageResult.of(
                socialMapper.selectAccessLogs((page - 1) * size, size),
                socialMapper.countAccessLogs(), page, size));
    }
}
