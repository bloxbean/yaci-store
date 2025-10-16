package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.mcp.server.aggregation.*;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.DynamicAggregationService;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.SchemaDiscoveryService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.mcp.server"})
public class StoreMcpServerConfig {

    @Bean
    public ToolCallbackProvider storeTools(
            @Autowired(required = false) McpUtxoService mcpUtxoService,
            @Autowired(required = false) McpBlockService mcpBlockService,
            @Autowired(required = false) McpTransactionService mcpTransactionService,
            @Autowired(required = false) McpAssetService mcpAssetService,
            @Autowired(required = false) McpEpochService mcpEpochService,
            @Autowired(required = false) McpEpochParamService mcpEpochParamService,
            @Autowired(required = false) McpStakingService mcpStakingService,
            @Autowired(required = false) McpAccountService mcpAccountService,
            @Autowired(required = false) McpAdaPotService mcpAdaPotService,
            @Autowired(required = false) McpGovernanceAggrService mcpGovernanceAggrService,
            @Autowired(required = false) McpMetadataService mcpMetadataService,
            @Autowired(required = false) McpDatumService mcpDatumService,
            @Autowired(required = false) McpCardanoUtilService mcpCardanoUtilService,
            // Aggregation Services (Phase 1-3)
            @Autowired(required = false) McpUtxoAggregationService utxoAggregationService,
            @Autowired(required = false) McpScriptAnalyticsService scriptAnalyticsService,
            @Autowired(required = false) McpTransactionAggregationService transactionAggregationService,
            @Autowired(required = false) McpBlockAggregationService blockAggregationService,
            @Autowired(required = false) McpAssetAggregationService assetAggregationService,
            @Autowired(required = false) McpPoolAggregationService poolAggregationService,
            @Autowired(required = false) SchemaDiscoveryService schemaDiscoveryService,
            @Autowired(required = false)DynamicAggregationService dynamicAggregationService
            ) {
        List<Object> toolObjects = new ArrayList<>();

        if (mcpUtxoService != null) toolObjects.add(mcpUtxoService);
        if (mcpBlockService != null) toolObjects.add(mcpBlockService);
        if (mcpTransactionService != null) toolObjects.add(mcpTransactionService);
        if (mcpAssetService != null) toolObjects.add(mcpAssetService);
        if (mcpEpochService != null) toolObjects.add(mcpEpochService);
        if (mcpEpochParamService != null) toolObjects.add(mcpEpochParamService);
        if (mcpStakingService != null) toolObjects.add(mcpStakingService);
        if (mcpAccountService != null) toolObjects.add(mcpAccountService);
        if (mcpAdaPotService != null) toolObjects.add(mcpAdaPotService);
        if (mcpGovernanceAggrService != null) toolObjects.add(mcpGovernanceAggrService);
        if (mcpMetadataService != null) {
            log.info("Registering McpMetadataService");
            toolObjects.add(mcpMetadataService);
        }
        if (mcpDatumService != null) {
            log.info("Registering McpDatumService");
            toolObjects.add(mcpDatumService);
        }
        if (mcpCardanoUtilService != null) {
            log.info("Registering McpCardanoUtilService");
            toolObjects.add(mcpCardanoUtilService);
        }

        // Register aggregation services
        if (utxoAggregationService != null) toolObjects.add(utxoAggregationService);
        if (scriptAnalyticsService != null) toolObjects.add(scriptAnalyticsService);
        if (transactionAggregationService != null) toolObjects.add(transactionAggregationService);
        if (blockAggregationService != null) toolObjects.add(blockAggregationService);
        if (assetAggregationService != null) toolObjects.add(assetAggregationService);
        if (poolAggregationService != null) toolObjects.add(poolAggregationService);

        if (schemaDiscoveryService != null) toolObjects.add(schemaDiscoveryService);
        if (dynamicAggregationService != null) toolObjects.add(dynamicAggregationService);

        return MethodToolCallbackProvider.builder()
                .toolObjects(toolObjects.toArray(new Object[0]))
                .build();
    }

    /**
     * Shutdown hook to ensure MCP server resources are cleaned up properly.
     * This helps with graceful shutdown when Ctrl+C is pressed.
     */
    @PreDestroy
    public void onShutdown() {
        log.info("Shutting down MCP Server - cleaning up resources...");
        // Spring AI MCP server handles its own cleanup
        // This hook ensures the shutdown is logged and any future cleanup can be added here
        log.info("MCP Server shutdown complete");
    }
}
