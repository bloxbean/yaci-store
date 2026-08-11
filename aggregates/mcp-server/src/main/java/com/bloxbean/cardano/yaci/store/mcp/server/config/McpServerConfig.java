package com.bloxbean.cardano.yaci.store.mcp.server.config;

import com.bloxbean.cardano.yaci.store.mcp.server.analytics.McpAnalyticsService;
import com.bloxbean.cardano.yaci.store.mcp.server.analytics.McpBalanceService;
import com.bloxbean.cardano.yaci.store.mcp.server.dapp.McpDAppRegistryService;
import com.bloxbean.cardano.yaci.store.mcp.server.external.McpExternalMetadataService;
import com.bloxbean.cardano.yaci.store.mcp.server.util.McpCardanoUtilService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP server configuration — registers all tool-providing services.
 *
 * <p>Each service is injected with {@code @Autowired(required = false)} so tools
 * are only registered when their dependencies are satisfied. For example, analytics
 * tools require {@code yaci.store.analytics.query.enabled=true}.</p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "yaci.store.mcp-server", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.bloxbean.cardano.yaci.store.mcp.server")
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(
            @Autowired(required = false) McpAnalyticsService analyticsService,
            @Autowired(required = false) McpBalanceService balanceService,
            @Autowired(required = false) McpCardanoUtilService utilService,
            @Autowired(required = false) McpDAppRegistryService dappService,
            @Autowired(required = false) McpExternalMetadataService metadataService
    ) {
        List<Object> toolObjects = new ArrayList<>();

        if (analyticsService != null) {
            log.info("Registering MCP analytics tools (list-tables, describe-table, execute-sql)");
            toolObjects.add(analyticsService);
        }
        if (balanceService != null) {
            log.info("Registering MCP balance tools (address-balance, top-balances)");
            toolObjects.add(balanceService);
        }
        if (utilService != null) {
            log.info("Registering MCP Cardano utility tools");
            toolObjects.add(utilService);
        }
        if (dappService != null) {
            log.info("Registering MCP DApp registry tools");
            toolObjects.add(dappService);
        }
        if (metadataService != null) {
            log.info("Registering MCP external metadata tools");
            toolObjects.add(metadataService);
        }

        log.info("MCP server: registered {} tool service(s)", toolObjects.size());

        return MethodToolCallbackProvider.builder()
                .toolObjects(toolObjects.toArray(new Object[0]))
                .build();
    }
}
