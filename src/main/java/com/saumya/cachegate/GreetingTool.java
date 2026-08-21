package com.saumya.cachegate;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

@Service
public class GreetingTool {

    @McpTool
    public String sayHello(
            @McpToolParam(description = "The name of the person to greet", required = true) String name
    ) {
        return "Hello " + name + "! CacheGate is alive.";
    }
}
