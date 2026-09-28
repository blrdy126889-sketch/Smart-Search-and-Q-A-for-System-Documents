package com.docqa.framework.llm;

import java.util.List;
import java.util.function.Consumer;

/**
 * LLM 客户端接口（OpenAI 兼容协议）
 */
public interface LlmClient {

    String chat(List<ChatMessage> messages);

    void chatStream(List<ChatMessage> messages, Consumer<String> onDelta, Consumer<int[]> onComplete);

    default boolean isMock() { return false; }

    record ChatMessage(String role, String content) {
        public static ChatMessage system(String content) { return new ChatMessage("system", content); }
        public static ChatMessage user(String content) { return new ChatMessage("user", content); }
        public static ChatMessage assistant(String content) { return new ChatMessage("assistant", content); }
    }
}
