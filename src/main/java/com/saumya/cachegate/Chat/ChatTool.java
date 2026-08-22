package com.saumya.cachegate.Chat;

import com.saumya.cachegate.cache.CacheEntry;
import com.saumya.cachegate.cache.ScoredEntry;
import com.saumya.cachegate.cache.SemanticCache;
import com.saumya.cachegate.llmProvider.ProviderChain;
import com.saumya.cachegate.llmProvider.budget.RequestBudget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    @McpTool(description = "Send a prompt to an LLM. May return a direct answer, or — if similar past questions exist — a list of candidates for the user to confirm before answering.")
    public String chatCompletion(
            @McpToolParam(description = "The prompt to send to the model", required = true) String prompt,
            @McpToolParam(description = "If the user confirmed reusing a specific previously-suggested cached answer, pass its id here.", required = false) Long useCacheId,
            @McpToolParam(description = "Set true to skip the cache and force a fresh provider call — use after the user declined all suggested matches.", required = false) Boolean skipCache) {

        if (useCacheId != null) {
            Optional<CacheEntry> chosen = semanticCache.findById(useCacheId);
            if (chosen.isPresent()) {
                semanticCache.getHits();
                log.info("CACHE HIT (user-confirmed) id={} for prompt: \"{}\"", useCacheId, prompt);
                return chosen.get().response();
            }
            log.warn("useCacheId {} not found — falling through to normal flow", useCacheId);
        }

        float[] embedding = embeddingModel.embed(prompt);

        if (!Boolean.TRUE.equals(skipCache)) {
            List<ScoredEntry> candidates = semanticCache.findCandidates(embedding);

            if (!candidates.isEmpty() && semanticCache.isAutoAccept(candidates.get(0).score())) {
                semanticCache.getHits();
                log.info("CACHE HIT (auto, score={}) for prompt: \"{}\"", candidates.get(0).score(), prompt);
                return candidates.get(0).entry().response();
            }

            if (!candidates.isEmpty()) {
                log.info("CACHE CANDIDATES ({}) for prompt: \"{}\" — asking user to confirm", candidates.size(), prompt);
                return formatClarification(candidates);
            }
        } else {
            log.info("CACHE SKIPPED (user declined matches) for prompt: \"{}\"", prompt);
        }

        log.info("CACHE MISS for prompt: \"{}\" — calling provider chain", prompt);
        requestBudget.consume();
        String response = providerChain.complete(prompt);
        semanticCache.store(prompt, embedding, response);

        return response;
    }

    /**
     * Formats a list of candidates into a string for user clarification.
     */
    private String formatClarification(List<ScoredEntry> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("Found similar past question(s). Ask the user which one (if any) already answers their question. ")
                .append("If they pick one, call chatCompletion again with the same prompt and useCacheId set to that id. ")
                .append("If none match, call again with skipCache set to true.\n\n");
        for (ScoredEntry c : candidates) {
            sb.append(String.format("id=%d (similarity %.2f)%nQ: %s%nA: %s%n%n",
                    c.entry().id(), c.score(), c.entry().prompt(), truncate(c.entry().response(), 220)));
        }

        return sb.toString();
    }

    /**
     * Truncates a string to a specified length, adding ellipsis if the string is longer.
     */
    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}