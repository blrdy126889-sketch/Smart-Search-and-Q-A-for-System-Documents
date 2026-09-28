package com.docqa.qa;

import com.docqa.common.api.R;
import com.docqa.common.exception.BizException;
import com.docqa.common.util.SecurityUtils;
import com.docqa.config.DocQaProperties;
import com.docqa.qa.entity.BizQaLog;
import com.docqa.qa.mapper.BizQaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

/**
 * RAG 智能问答接口：SSE 流式 / 会话管理 / 反馈
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/qa")
@RequiredArgsConstructor
public class QaController {

    private final BizQaMapper qaMapper;
    private final QaStreamService streamService;
    private final DocQaProperties properties;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody Map<String, Object> body) {
        long userId = SecurityUtils.userId();
        String sessionId = String.valueOf(body.get("sessionId"));
        String question = String.valueOf(body.get("question")).trim();
        if (question.isEmpty() || question.length() > 500) {
            throw new BizException("问题不能为空且不超过 500 字");
        }
        if (!qaMapper.ownsSession(sessionId, userId)) {
            throw new BizException(403, "无权访问该会话");
        }

        if (qaMapper.selectMessages(sessionId).isEmpty()) {
            qaMapper.updateSessionTitle(sessionId,
                    question.length() > 50 ? question.substring(0, 50) : question);
        }

        BizQaLog qaLog = new BizQaLog();
        qaLog.setSessionId(sessionId);
        qaLog.setUserId(userId);
        qaLog.setQuestion(question);
        qaLog.setModel(properties.getLlm().getModel());
        qaLog.setSources("[]");
        qaMapper.insertQaLog(qaLog);

        SseEmitter emitter = new SseEmitter(180_000L);
        streamService.stream(emitter, sessionId, userId, question, qaLog.getId());
        return emitter;
    }

    @PostMapping("/sessions")
    public R<Map<String, Object>> createSession(@RequestBody(required = false) Map<String, Object> body) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String title = body == null ? null : String.valueOf(body.getOrDefault("title", ""));
        boolean hasTitle = title != null && !"null".equals(title) && !title.isBlank();
        qaMapper.insertSession(sessionId, SecurityUtils.userId(), hasTitle ? title : null);
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("title", hasTitle ? title : "新对话");
        return R.ok(data);
    }

    @GetMapping("/sessions")
    public R<List<Map<String, Object>>> sessions() {
        List<Map<String, Object>> rows = qaMapper.selectSessions(SecurityUtils.userId());
        for (Map<String, Object> row : rows) {
            row.put("sessionId", row.remove("session_id"));
            row.put("createdAt", row.remove("created_at"));
            row.put("updatedAt", row.remove("updated_at"));
        }
        return R.ok(rows);
    }

    @DeleteMapping("/sessions/{id}")
    public R<Void> deleteSession(@PathVariable String id) {
        qaMapper.deleteSession(id, SecurityUtils.userId());
        return R.ok();
    }

    @GetMapping("/sessions/{id}/messages")
    public R<List<Map<String, Object>>> messages(@PathVariable String id) {
        if (!qaMapper.ownsSession(id, SecurityUtils.userId())) {
            throw new BizException(403, "无权访问该会话");
        }
        List<Map<String, Object>> rows = qaMapper.selectMessages(id);
        for (Map<String, Object> row : rows) {
            row.put("qaId", row.remove("qa_id"));
        }
        return R.ok(rows);
    }

    @PostMapping("/{qaId}/feedback")
    public R<Void> feedback(@PathVariable Long qaId, @RequestBody Map<String, Object> body) {
        int feedback = ((Number) body.get("feedback")).intValue();
        if (feedback != 1 && feedback != -1) throw new BizException("feedback 仅支持 1 或 -1");
        BizQaLog log = qaMapper.selectById(qaId);
        if (log == null || !log.getUserId().equals(SecurityUtils.userId())) {
            throw new BizException(403, "无权操作该问答记录");
        }
        qaMapper.updateFeedback(qaId, feedback);
        return R.ok();
    }
}
