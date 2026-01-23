package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.store.adminui.dto.StoreStatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to retrieve store status using Spring Environment property lookup.
 * This implementation avoids reflection for GraalVM native image compatibility.
 */
@Service
@Slf4j
public class StoreStatusService {

    private final Environment environment;

    // Static list of store configurations - no reflection needed
    private static final List<StoreConfig> STORES = List.of(
            new StoreConfig("Utxo", "store.utxo.enabled", "store.utxo.api-enabled", true),
            new StoreConfig("Blocks", "store.blocks.enabled", "store.blocks.api-enabled", true),
            new StoreConfig("Transaction", "store.transaction.enabled", "store.transaction.api-enabled", true),
            new StoreConfig("Script", "store.script.enabled", "store.script.api-enabled", true),
            new StoreConfig("Metadata", "store.metadata.enabled", "store.metadata.api-enabled", true),
            new StoreConfig("Assets", "store.assets.enabled", "store.assets.api-enabled", true),
            new StoreConfig("Epoch", "store.epoch.enabled", "store.epoch.api-enabled", true),
            new StoreConfig("Staking", "store.staking.enabled", "store.staking.api-enabled", true),
            new StoreConfig("MIR", "store.mir.enabled", "store.mir.api-enabled", true),
            new StoreConfig("Governance", "store.governance.enabled", "store.governance.api-enabled", true),
            new StoreConfig("Submit", "store.submit.enabled", null, true),  // Submit has no api-enabled property, defaults to enabled
            new StoreConfig("Account", "store.account.enabled", "store.account.api-enabled", false),
            new StoreConfig("Epoch Aggregation", "store.epoch-aggr.enabled", "store.epoch-aggr.api-enabled", false),
            new StoreConfig("AdaPot", "store.adapot.enabled", "store.adapot.api-enabled", false),
            new StoreConfig("Governance Aggregation", "store.governance-aggr.enabled", "store.governance-aggr.api-enabled", false),
            new StoreConfig("Live", "store.live.enabled", null, false),  // Live has no api-enabled property
            new StoreConfig("Analytics", "store.analytics.enabled", "store.analytics.api-enabled", false),
            new StoreConfig("Remote", "store.remote.enabled", "store.remote.api-enabled", false),
            new StoreConfig("MCP Server", "store.mcp-server.enabled", "store.mcp-server.api-enabled", false)
    );

    public StoreStatusService(Environment environment) {
        this.environment = environment;
    }

    public List<StoreStatusDto> getStoreStatuses() {
        List<StoreStatusDto> stores = new ArrayList<>();

        for (StoreConfig config : STORES) {
            boolean enabled = environment.getProperty(config.enabledProperty(), Boolean.class, config.defaultEnabled());
            boolean apiEnabled = config.apiEnabledProperty() != null
                    ? environment.getProperty(config.apiEnabledProperty(), Boolean.class, true)
                    : true;

            stores.add(StoreStatusDto.builder()
                    .name(config.displayName())
                    .enabled(enabled)
                    .apiEnabled(apiEnabled)
                    .build());
        }

        return stores;
    }

    private record StoreConfig(String displayName, String enabledProperty, String apiEnabledProperty, boolean defaultEnabled) {}
}
