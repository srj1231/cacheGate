package com.saumya.cachegate.llmProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Gemini provider for LLM services.
 */
@Component
@Order(1)
public class GeminiProvider implements LlmProvider {

    private final ChatClient chatClient;

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    public GeminiProvider(@Qualifier("googleGenAiChatModel")ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public String complete(String prompt) {
        log.info("GEMINI CHAT CALL FIRING for prompt: \"{}\"", prompt);
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
