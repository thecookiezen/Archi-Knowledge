package com.thecookiezen.archiledger.infrastructure.config;

import com.thecookiezen.archiledger.infrastructure.mcp.McpToolAdapter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpToolsConfig {

    @Bean
    public List<ToolCallback> mcpTools(McpToolAdapter mcpToolAdapter) {
        return List.of(ToolCallbacks.from(mcpToolAdapter));
    }
}
