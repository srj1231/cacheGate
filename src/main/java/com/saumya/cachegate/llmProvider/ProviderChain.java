package com.saumya.cachegate.llmProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProviderChain {

    private static final Logger log = LoggerFactory.getLogger(ProviderChain.class);

    private final List<LlmProvider> providers;
    private final Map<String, ProviderStatus> statusMap = new ConcurrentHashMap<>();

    public record ProviderStatus(String outcome, Instant at) {}

    public ProviderChain(List<LlmProvider> providers) {
        this.providers = providers;
    }

    public String complete(String prompt) {
        RuntimeException lastError = null;

        for (LlmProvider provider : providers) {
            String name = provider.getClass().getSimpleName();
            try {
                log.info("PROVIDER ATTEMPT: {}", name);
                String result = provider.complete(prompt);
                statusMap.put(name, new ProviderStatus("OK", Instant.now()));
                return result;
            } catch (RuntimeException e) {
                statusMap.put(name, new ProviderStatus("FAILED: " + e.getMessage(), Instant.now()));
                log.warn("PROVIDER FAILED: {} — {} — trying next in chain", name, e.getMessage());
                lastError = e;
            }
        }
        throw new IllegalStateException("All providers in the chain failed", lastError);
    }

    public List<String> getProviderNamesInOrder() {
        return providers.stream().map(p -> p.getClass().getSimpleName()).toList();
    }

    public Map<String, ProviderStatus> getStatus() {
        return statusMap;
    }
}
