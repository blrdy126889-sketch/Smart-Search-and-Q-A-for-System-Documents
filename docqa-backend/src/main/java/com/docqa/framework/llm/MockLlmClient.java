package com.docqa.framework.llm;

import com.docqa.config.DocQaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Mock LLM：未配置 API Key 时启用。基于检索上下文模板化流式输出，完整模拟 RAG 推流链路。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${docqa.llm.api-key:}' == ''")
public class MockLlmClient implements LlmClient {

    private final DocQaProperties properties;

    @Override
    public String chat(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        chatStream(messages, sb::append, u -> { });
        return sb.toString();
    }

    @Override
    public void chatStream(List<ChatMessage> messages, Consumer<String> onDelta, Consumer<int[]> onComplete) {
        String context = messages.stream()
                .filter(m -> "system".equals(m.role()))
                .map(ChatMessage::content)
                .findFirst()
                .orElse("");
        String userQ = messages.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((first, second) -> second)
                .map(ChatMessage::content)
                .orElse("您的问题");

        String answer = buildAnswer(userQ, context);
        for (int i = 0; i < answer.length(); i += 4) {
            onDelta.accept(answer.substring(i, Math.min(answer.length(), i + 4)));
            try { Thread.sleep(30); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        int promptTokens = context.length() / 2 + userQ.length();
        onComplete.accept(new int[]{promptTokens, answer.length() / 2});
    }

    private String buildAnswer(String question, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据现有制度库检索结果，针对「")
          .append(question.length() > 30 ? question.substring(0, 30) + "…" : question)
          .append("」回答如下：\n\n");
        if (context.contains("[1]")) {
            sb.append("1. 相关制度条款要点见参考资料 [1]");
            if (context.contains("[2]")) sb.append("，补充说明见 [2]");
            if (context.contains("[3]")) sb.append("，[3] 亦有相关规定");
            sb.append("。\n\n");
        }
        sb.append("**摘要说明**：当前为演示模式（未配置大模型 API Key），以上内容展示了 RAG 检索增强问答的完整链路——\n");
        sb.append("- 系统已对您的问题执行 **BM25 关键词 + 语义向量** 双路混合检索；\n");
        sb.append("- 检索到的 Top-K 切片已作为上下文注入提示词，并以 [n] 标注来源；\n");
        sb.append("- 答案以 SSE 流式推送，前端呈现打字机效果。\n\n");
        sb.append("配置 `LLM_API_KEY` 环境变量后，本链路将无缝切换为真实大模型生成。\n\n");
        sb.append("> 免责声明：制度条款以正式发布文档为准，请点击来源卡片查看原文。");
        return sb.toString();
    }

    @Override
    public boolean isMock() { return true; }
}
