package com.saumya.cachegate.Chat;

import com.saumya.cachegate.cache.SemanticCache;
import com.saumya.cachegate.llmProvider.ProviderChain;
import com.saumya.cachegate.llmProvider.budget.RequestBudget;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Chat tool for LLM services.
 */
@Service
public class ChatTool {

    private static final Logger log = LoggerFactory.getLogger(ChatTool.class);

    private final ProviderChain providerChain;
    private final EmbeddingModel embeddingModel;
    private final SemanticCache semanticCache;
    private final RequestBudget requestBudget;

    public ChatTool(ProviderChain providerChain,
                    @Qualifier("googleGenAiTextEmbedding") EmbeddingModel embeddingModel,
                    SemanticCache semanticCache,
                    RequestBudget requestBudget) {
        this.providerChain = providerChain;
        this.embeddingModel = embeddingModel;
        this.semanticCache = semanticCache;
        this.requestBudget = requestBudget;
    }

    @McpTool(description = "Send a prompt to an LLM and get back a completion, using a semantic cache to avoid duplicate calls.")
    public String chatCompletion(
            @McpToolParam(description = "The prompt to send to the model", required = true) String prompt
    ) {
        float[] embedding = embeddingModel.embed(prompt);

        Optional<String> cache = semanticCache.findSimilar(embedding);
        if(cache.isPresent()) {
            log.info("CACHE HIT for prompt: \"{}\"", prompt);
            return cache.get();
        }

        log.info("CACHE MISS for prompt: \"{}\" — calling provider chain", prompt);
        requestBudget.consume();
        String response = providerChain.complete(prompt);
        semanticCache.store(prompt, embedding, response);

        return response;
    }
}
