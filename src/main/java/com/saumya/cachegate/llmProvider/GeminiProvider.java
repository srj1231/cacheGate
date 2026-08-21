package com.saumya.cachegate.llmProvider;

import com.saumya.cachegate.cache.SemanticCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Gemini provider for LLM services.
 */
@Component
public class GeminiProvider implements LlmProvider {

    private final ChatClient chatClient;

    private static final Logger log = LoggerFactory.getLogger(SemanticCache.class);

    public GeminiProvider(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
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
