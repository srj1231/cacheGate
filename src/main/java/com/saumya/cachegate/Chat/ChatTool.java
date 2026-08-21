package com.saumya.cachegate.Chat;

import com.saumya.cachegate.llmProvider.LlmProvider;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * Chat tool for LLM services.
 */
@Service
public class ChatTool {

    private final LlmProvider llmProvider;

    public ChatTool(LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    @McpTool(description = "Send a prompt to an LLM and get back a completion")
    public String chatCompletion(
            @McpToolParam(description = "The prompt to send to the model", required = true) String prompt
    ) {
        return llmProvider.complete(prompt);
    }
}
