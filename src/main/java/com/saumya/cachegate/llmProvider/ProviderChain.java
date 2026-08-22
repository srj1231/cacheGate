package com.saumya.cachegate.llmProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProviderChain {

    private static final Logger log = LoggerFactory.getLogger(ProviderChain.class);

    private final List<LlmProvider> providers; // Spring injects this pre-sorted by @Order

    public ProviderChain(List<LlmProvider> providers) {
        this.providers = providers;
    }

    public String complete(String prompt) {
        RuntimeException lastError = null;

        for (LlmProvider provider : providers) {
            String name = provider.getClass().getSimpleName();
            try {
                log.info("PROVIDER ATTEMPT: {}", name);
                return provider.complete(prompt);
            } catch (RuntimeException e) {
                log.warn("PROVIDER FAILED: {} — {} — trying next in chain", name, e.getMessage());
                lastError = e;
            }
        }
        throw new IllegalStateException("All providers in the chain failed", lastError);
    }
}
