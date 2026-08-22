package com.saumya.cachegate.llmProvider.budget;

import com.saumya.cachegate.cache.SemanticCache;
import com.saumya.cachegate.llmProvider.ProviderChain;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminTools {

    private final SemanticCache semanticCache;
    private final ProviderChain providerChain;
    private final RequestBudget requestBudget;

    public AdminTools(SemanticCache cache, RequestBudget budget, ProviderChain providerChain) {
        this.semanticCache = cache;
        this.requestBudget = budget;
        this.providerChain = providerChain;
    }

    @McpTool(description = "Return cache hit-rate stats and current session budget usage")
    public String cacheStats() {
        int total = semanticCache.getTotalChecks();
        int hits = semanticCache.getHits();
        double hitRate = total == 0 ? 0.0 : (100.0 * hits / total);
        return String.format(
                "Cache entries: %d | Requests checked: %d | Cache hits: %d | Hit rate: %.1f%% | Provider calls saved: %d | Session budget: %d / %d used",
                semanticCache.getEntryCount(), total, hits, hitRate, hits, requestBudget.used(), requestBudget.max());
    }

    @McpTool(description = "List configured LLM providers in fallback order with their last known status this session")
    public String listProviders() {
        List<String> names = providerChain.getProviderNamesInOrder();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            var status = providerChain.getStatus().get(name);
            String statusText = status == null ? "not used yet this session" : status.outcome() + " at " + status.at();
            sb.append(i + 1).append(". ").append(name).append(" — ").append(statusText).append("\n");
        }
        return sb.toString();
    }
}
