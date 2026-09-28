package com.docqa.framework.llm;

import com.docqa.common.exception.BizException;
import com.docqa.config.DocQaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAI 协议兼容 LLM 客户端（GLM/DeepSeek/Qwen/vLLM），SSE 流式解析
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${docqa.llm.api-key:}' != ''")
public class OpenAiCompatLlmClient implements LlmClient {

    private final DocQaProperties properties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String chat(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        chatStream(messages, sb::append, usage -> { });
        return sb.toString();
    }

    @Override
    public void chatStream(List<ChatMessage> messages, Consumer<String> onDelta, Consumer<int[]> onComplete) {
        DocQaProperties.Llm cfg = properties.getLlm();
        Map<String, Object> body = new HashMap<>();
        body.put("model", cfg.getModel());
        body.put("messages", messages.stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList());
        body.put("stream", true);
        body.put("temperature", cfg.getTemperature());

        int[] usage = {0, 0};
        Flux<String> flux = webClientBuilder.build().post()
                .uri(cfg.getBaseUrl() + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(cfg.getTimeoutSeconds()));

        try {
            flux.doOnNext(chunk -> {
                        String data = chunk.startsWith("data:") ? chunk.substring(5).trim() : chunk;
                        if (data.isEmpty() || "[DONE]".equals(data)) return;
                        try {
                            JsonNode node = objectMapper.readTree(data);
                            JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                            if (!delta.isMissingNode() && !delta.isNull()) {
                                String text = delta.asText();
                                if (!text.isEmpty()) onDelta.accept(text);
                            }
                            JsonNode u = node.path("usage");
                            if (!u.isMissingNode()) {
                                usage[0] = u.path("prompt_tokens").asInt(0);
                                usage[1] = u.path("completion_tokens").asInt(0);
                            }
                        } catch (Exception ignore) { }
                    })
                    .blockLast();
            onComplete.accept(usage);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM 流式调用失败: {}", e.getMessage());
            throw new BizException("大模型服务调用失败，请稍后重试");
        }
    }
}
