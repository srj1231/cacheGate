package com.saumya.cachegate.llmProvider;

/**
 * Provider interface for LLM services.
 * Defines the contract for completing prompts using various LLM implementations.
 */
public interface LlmProvider {
    String complete(String prompt);
}
