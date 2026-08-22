package com.saumya.cachegate.llmProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * OpenRouter provider for LLM services.
 */
@Component
@Order(3)
public class OpenRouterProvider implements LlmProvider {

    private final ChatClient chatClient;

    private static final Logger log = LoggerFactory.getLogger(OpenRouterProvider.class);

    public OpenRouterProvider(
            @Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.base-url}") String baseUrl,
            @Value("${openrouter.model}") String model) {

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .model(model)
                        .build())
                .build();

        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public String complete(String prompt) {
        log.info("OPENROUTER CHAT CALL FIRING for prompt: \"{}\"", prompt);
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
