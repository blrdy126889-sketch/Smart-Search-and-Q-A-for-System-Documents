package com.docqa.qa;

import com.docqa.common.exception.BizException;
import com.docqa.config.DocQaProperties;
import com.docqa.document.mapper.BizDocumentMapper;
import com.docqa.document.mapper.BizDocSocialMapper;
import com.docqa.framework.llm.LlmClient;
import com.docqa.framework.llm.LlmClient.ChatMessage;
import com.docqa.qa.entity.BizQaLog;
import com.docqa.qa.mapper.BizQaMapper;
import com.docqa.search.HybridSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RAG 流式问答服务（独立 Bean 保证 @Async 代理生效）
 * SSE 事件：meta(来源) → delta(增量) → done / error
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QaStreamService {

    private final BizQaMapper qaMapper;
    private final BizDocumentMapper documentMapper;
    private final BizDocSocialMapper socialMapper;
    private final HybridSearchService searchService;
    private final LlmClient llmClient;
    private final DocQaProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            你是企业制度文档智能问答助手。回答规则：
            1. 仅依据下方【参考资料】回答问题，引用对应资料时在句末标注编号，如 [1] [2]；
            2. 若参考资料不足以回答，明确回复"根据现有制度库未找到相关规定"，禁止编造；
            3. 回答使用简体中文，条理清晰，重要数值（金额/天数/比例）必须与资料一致；
            4. 涉及流程审批的问题，按资料中的顺序逐步说明。""";

    @Async("qaStreamExecutor")
    public void stream(SseEmitter emitter, String sessionId, long userId, String question, Long qaLogId,
                       List<Long> roleIds) {
        long start = System.currentTimeMillis();
        AtomicInteger firstTokenMs = new AtomicInteger(0);
        StringBuilder answerBuf = new StringBuilder();
        String sourcesJson = "[]";
        Long qaId = qaLogId;

        try {
            // ① RAG 混合检索 Top-K
            List<Map<String, Object>> topK = new ArrayList<>();
            try {
                Map<String, Object> searchResult = searchService.search(question, "HYBRID", null, 1,
                        properties.getSearch().getQaTopK(), roleIds);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> recs = (List<Map<String, Object>>) searchResult.getOrDefault("records", List.of());
                topK = recs;
            } catch (Exception e) {
                log.warn("RAG 检索失败，降级为无上下文问答: {}", e.getMessage());
            }

            // ② meta 事件：来源列表
            List<Map<String, Object>> sources = new ArrayList<>();
            StringBuilder context = new StringBuilder();
            for (int i = 0; i < topK.size(); i++) {
                Map<String, Object> hit = topK.get(i);
                Map<String, Object> src = new LinkedHashMap<>();
                src.put("no", i + 1);
                src.put("docId", hit.get("docId"));
                src.put("docTitle", hit.get("docTitle"));
                src.put("headingPath", hit.get("headingPath"));
                src.put("score", hit.get("score"));
                String snippet = String.valueOf(hit.get("snippet"));
                src.put("snippet", snippet.length() > 160 ? snippet.substring(0, 160) : snippet);
                sources.add(src);
                context.append("[").append(i + 1).append("] 《").append(hit.get("docTitle")).append("》")
                        .append(hit.get("headingPath") == null ? "" : " " + hit.get("headingPath"))
                        .append("\n").append(hit.get("snippet")).append("\n\n");
            }
            sourcesJson = objectMapper.writeValueAsString(sources);
            send(emitter, "meta", Map.of("sources", sources));

            // ③ Prompt 组装
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.system(SYSTEM_PROMPT + "\n\n【参考资料】\n"
                    + (context.isEmpty() ? "（未检索到相关制度条款）" : context)));
            for (Map<String, Object> m : recentHistory(sessionId)) {
                messages.add(new ChatMessage(String.valueOf(m.get("role")), String.valueOf(m.get("content"))));
            }
            messages.add(ChatMessage.user(question));

            // ④ LLM 流式 → delta
            long[] usage = {0, 0};
            StringBuilder frame = new StringBuilder();
            llmClient.chatStream(messages, delta -> {
                if (firstTokenMs.get() == 0) firstTokenMs.set((int) (System.currentTimeMillis() - start));
                answerBuf.append(delta);
                frame.append(delta);
                if (frame.length() >= 8) {
                    send(emitter, "delta", Map.of("content", frame.toString()));
                    frame.setLength(0);
                }
            }, u -> { usage[0] = u[0]; usage[1] = u[1]; });
            if (frame.length() > 0) {
                send(emitter, "delta", Map.of("content", frame.toString()));
            }

            // ⑤ T5 收尾：qa_log DONE + 引用计数 + QUOTE 日志 + done 事件
            BizQaLog finish = new BizQaLog();
            finish.setId(qaId);
            finish.setAnswer(answerBuf.toString());
            finish.setStatus("DONE");
            finish.setLatencyMs((int) (System.currentTimeMillis() - start));
            finish.setFirstTokenMs(firstTokenMs.get());
            finish.setPromptTokens((int) usage[0]);
            finish.setCompletionTokens((int) usage[1]);
            finish.setSources(sourcesJson.toString());
            qaMapper.finishQaLog(finish);
            for (Map<String, Object> hit : topK) {
                try {
                    Long docId = ((Number) hit.get("docId")).longValue();
                    documentMapper.increaseQuoteCount(docId);
                    socialMapper.insertAccessLog(userId, docId, "QUOTE", question, null);
                } catch (Exception ignore) { }
            }
            send(emitter, "done", Map.of("qaId", qaId, "firstTokenMs", firstTokenMs.get()));
            emitter.complete();
        } catch (Exception e) {
            log.error("RAG 问答失败: {}", e.getMessage(), e);
            try { qaMapper.failQaLog(qaId); } catch (Exception ignore) { }
            try {
                send(emitter, "error", Map.of("code", 500, "msg", "生成失败：" + e.getMessage()));
            } catch (Exception ignore) { }
            emitter.complete();
        }
    }

    private List<Map<String, Object>> recentHistory(String sessionId) {
        List<Map<String, Object>> msgs = qaMapper.selectMessages(sessionId);
        if (msgs.size() <= 1) return List.of();
        List<Map<String, Object>> history = new ArrayList<>(msgs.subList(0, msgs.size() - 1));
        int from = Math.max(0, history.size() - 6);
        return history.subList(from, history.size());
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(objectMapper.writeValueAsString(data)));
        } catch (Exception e) {
            throw new BizException("SSE 推送失败（客户端可能已断开）");
        }
    }
}
