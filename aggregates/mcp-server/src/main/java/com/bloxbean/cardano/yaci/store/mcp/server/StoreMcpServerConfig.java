package com.bloxbean.cardano.yaci.store.mcp.server;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.mcp.server"})
public class StoreMcpServerConfig {

    @Bean
    public ToolCallbackProvider weatherTools(McpUtxoService mcpUtxoService) {
        return  MethodToolCallbackProvider.builder().toolObjects(mcpUtxoService).build();
    }
}
