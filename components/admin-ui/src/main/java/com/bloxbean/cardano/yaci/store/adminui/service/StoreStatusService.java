package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.store.adminui.dto.StoreStatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to retrieve store status using Spring Environment property lookup and class presence check.
 * This implementation avoids reflection for GraalVM native image compatibility.
 */
@Service
@Slf4j
public class StoreStatusService {

    private final Environment environment;

    // Static list of store configurations
    private static final List<StoreConfig> STORES = List.of(
            new StoreConfig("Utxo", "store.utxo.enabled", "store.utxo.api-enabled", true, "com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties"),
            new StoreConfig("Blocks", "store.blocks.enabled", "store.blocks.api-enabled", true, "com.bloxbean.cardano.yaci.store.blocks.BlocksStoreProperties"),
            new StoreConfig("Transaction", "store.transaction.enabled", "store.transaction.api-enabled", true, "com.bloxbean.cardano.yaci.store.transaction.TransactionStoreProperties"),
            new StoreConfig("Script", "store.script.enabled", "store.script.api-enabled", true, "com.bloxbean.cardano.yaci.store.script.ScriptStoreConfiguration"),
            new StoreConfig("Metadata", "store.metadata.enabled", "store.metadata.api-enabled", true, "com.bloxbean.cardano.yaci.store.metadata.MetadataStoreConfiguration"),
            new StoreConfig("Assets", "store.assets.enabled", "store.assets.api-enabled", true, "com.bloxbean.cardano.yaci.store.assets.AssetsStoreConfiguration"),
            new StoreConfig("Epoch", "store.epoch.enabled", "store.epoch.api-enabled", true, "com.bloxbean.cardano.yaci.store.epoch.EpochStoreConfiguration"),
            new StoreConfig("Staking", "store.staking.enabled", "store.staking.api-enabled", true, "com.bloxbean.cardano.yaci.store.staking.StakingStoreConfiguration"),
            new StoreConfig("MIR", "store.mir.enabled", "store.mir.api-enabled", true, "com.bloxbean.cardano.yaci.store.mir.MIRStoreConfiguration"),
            new StoreConfig("Governance", "store.governance.enabled", "store.governance.api-enabled", true, "com.bloxbean.cardano.yaci.store.governance.GovernanceStoreConfiguration"),
            new StoreConfig("Submit", "store.submit.enabled", null, true, "com.bloxbean.cardano.yaci.store.submit.SubmitStoreConfiguration"),
            new StoreConfig("Account", "store.account.enabled", "store.account.api-enabled", false, "com.bloxbean.cardano.yaci.store.account.AccountStoreProperties"),
            new StoreConfig("Epoch Aggregation", "store.epoch-aggr.enabled", "store.epoch-aggr.api-enabled", false, "com.bloxbean.cardano.yaci.store.epochaggr.EpochAggrConfiguration"),
            new StoreConfig("AdaPot", "store.adapot.enabled", "store.adapot.api-enabled", false, "com.bloxbean.cardano.yaci.store.adapot.AdaPotProperties"),
            new StoreConfig("Governance Aggregation", "store.governance-aggr.enabled", "store.governance-aggr.api-enabled", false, "com.bloxbean.cardano.yaci.store.governanceaggr.GovernanceAggrProperties")

            //Will be enabled in future release
//            new StoreConfig("Analytics", "store.analytics.enabled", "store.analytics.api-enabled", false, "com.bloxbean.cardano.yaci.store.analytics.AnalyticsStoreProperties"),
//            new StoreConfig("Remote", "store.remote.enabled", "store.remote.api-enabled", false, "com.bloxbean.cardano.yaci.store.remote.RemoteProperties"),
//            new StoreConfig("MCP Server", "store.mcp-server.enabled", "store.mcp-server.api-enabled", false, "com.bloxbean.cardano.yaci.store.mcp.McpServerProperties")
    );

    public StoreStatusService(Environment environment) {
        this.environment = environment;
    }

    public List<StoreStatusDto> getStoreStatuses() {
        List<StoreStatusDto> stores = new ArrayList<>();

        for (StoreConfig config : STORES) {
            if (!ClassUtils.isPresent(config.markerClassName(), null)) {
                continue;
            }

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

    private record StoreConfig(String displayName, String enabledProperty, String apiEnabledProperty, boolean defaultEnabled, String markerClassName) {}
}
