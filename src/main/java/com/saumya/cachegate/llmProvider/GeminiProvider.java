package com.saumya.cachegate.llmProvider;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Gemini provider for LLM services.
 */
@Component
public class GeminiProvider implements LlmProvider {

    private final ChatClient chatClient;

    public GeminiProvider(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String complete(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
