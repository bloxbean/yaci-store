package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.store.adminui.dto.ConfigSectionDto;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationService {
    private final StoreProperties storeProperties;
    private final Environment environment;

    public List<ConfigSectionDto> getConfiguration() {
        List<ConfigSectionDto> sections = new ArrayList<>();

        // Cardano Network Section
        Map<String, Object> cardanoProps = new LinkedHashMap<>();
        cardanoProps.put("store.cardano.host", maskIfEmpty(storeProperties.getCardanoHost()));
        cardanoProps.put("store.cardano.port", storeProperties.getCardanoPort());
        cardanoProps.put("store.cardano.protocol-magic", storeProperties.getProtocolMagic());
        cardanoProps.put("store.cardano.n2c-node-socket-path", maskIfEmpty(storeProperties.getN2cNodeSocketPath()));
        cardanoProps.put("store.cardano.n2c-host", maskIfEmpty(storeProperties.getN2cHost()));
        cardanoProps.put("store.cardano.n2c-port", storeProperties.getN2cPort());
        cardanoProps.put("store.cardano.is-mainnet", storeProperties.isMainnet());
        sections.add(ConfigSectionDto.builder()
                .name("Cardano Network")
                .properties(cardanoProps)
                .build());

        // Sync Configuration Section
        Map<String, Object> syncProps = new LinkedHashMap<>();
        syncProps.put("store.sync-auto-start", storeProperties.isSyncAutoStart());
        syncProps.put("store.sync-start-slot", storeProperties.getSyncStartSlot());
        syncProps.put("store.sync-start-blockhash", maskIfEmpty(storeProperties.getSyncStartBlockhash()));
        syncProps.put("store.sync-stop-slot", storeProperties.getSyncStopSlot());
        syncProps.put("store.sync-stop-blockhash", maskIfEmpty(storeProperties.getSyncStopBlockhash()));
        syncProps.put("store.block-diff-to-start-sync-protocol", storeProperties.getBlockDiffToStartSyncProtocol());
        sections.add(ConfigSectionDto.builder()
                .name("Sync Configuration")
                .properties(syncProps)
                .build());

        // Executor Configuration Section
        Map<String, Object> executorProps = new LinkedHashMap<>();
        executorProps.put("store.executor.block-processing-threads", storeProperties.getBlockProcessingThreads());
        executorProps.put("store.executor.event-processing-threads", storeProperties.getEventProcessingThreads());
        executorProps.put("store.executor.enable-parallel-processing", storeProperties.isEnableParallelProcessing());
        executorProps.put("store.executor.blocks-batch-size", storeProperties.getBlocksBatchSize());
        executorProps.put("store.executor.blocks-partition-size", storeProperties.getBlocksPartitionSize());
        executorProps.put("store.executor.use-virtual-thread-for-batch-processing", storeProperties.isUseVirtualThreadForBatchProcessing());
        executorProps.put("store.executor.use-virtual-thread-for-event-processing", storeProperties.isUseVirtualThreadForEventProcessing());
        executorProps.put("store.executor.processing-threads-timeout", storeProperties.getProcessingThreadsTimeout() + " min");
        sections.add(ConfigSectionDto.builder()
                .name("Executor Configuration")
                .properties(executorProps)
                .build());

        // Database Configuration Section
        Map<String, Object> dbProps = new LinkedHashMap<>();
        dbProps.put("store.db.batch-size", storeProperties.getDbBatchSize());
        dbProps.put("store.db.parallel-insert", storeProperties.isDbParallelInsert());
        dbProps.put("store.db.parallel-write", storeProperties.isParallelWrite());
        dbProps.put("store.db.write-thread-count", storeProperties.getWriteThreadCount());
        dbProps.put("store.db.write-thread-default-batch-size", storeProperties.getWriteThreadDefaultBatchSize());
        dbProps.put("store.db.jooq-write-batch-size", storeProperties.getJooqWriteBatchSize());
        sections.add(ConfigSectionDto.builder()
                .name("Database Configuration")
                .properties(dbProps)
                .build());

        // Cursor Configuration Section
        Map<String, Object> cursorProps = new LinkedHashMap<>();
        cursorProps.put("store.cursor.no-of-blocks-to-keep", storeProperties.getCursorNoOfBlocksToKeep());
        cursorProps.put("store.cursor.cleanup-interval", storeProperties.getCursorCleanupInterval() + " sec");
        sections.add(ConfigSectionDto.builder()
                .name("Cursor Configuration")
                .properties(cursorProps)
                .build());

        // N2C Pool Configuration Section
        Map<String, Object> n2cPoolProps = new LinkedHashMap<>();
        n2cPoolProps.put("store.cardano.n2c-pool-enabled", storeProperties.isN2cPoolEnabled());
        n2cPoolProps.put("store.cardano.n2c-pool-max-total", storeProperties.getN2cPoolMaxTotal());
        n2cPoolProps.put("store.cardano.n2c-pool-min-idle", storeProperties.getN2cPoolMinIdle());
        n2cPoolProps.put("store.cardano.n2c-pool-max-idle", storeProperties.getN2cPoolMaxIdle());
        n2cPoolProps.put("store.cardano.n2c-pool-max-wait-millis", storeProperties.getN2cPoolMaxWaitInMillis() + " ms");
        sections.add(ConfigSectionDto.builder()
                .name("N2C Pool Configuration")
                .properties(n2cPoolProps)
                .build());

        // Plugin Configuration Section
        Map<String, Object> pluginProps = new LinkedHashMap<>();
        pluginProps.put("store.plugins.enabled", storeProperties.isPluginsEnabled());
        pluginProps.put("store.plugins.exit-on-error", storeProperties.isPluginExitOnError());
        pluginProps.put("store.plugins.metrics-enabled", storeProperties.isPluginMetricsEnabled());
        pluginProps.put("store.plugins.api-enabled", storeProperties.isPluginApiEnabled());
        sections.add(ConfigSectionDto.builder()
                .name("Plugin Configuration")
                .properties(pluginProps)
                .build());

        // Other Configuration Section
        Map<String, Object> otherProps = new LinkedHashMap<>();
        otherProps.put("store.read-only-mode", storeProperties.isReadOnlyMode());
        otherProps.put("store.primary-instance", storeProperties.isPrimaryInstance());
        otherProps.put("store.metrics.enabled", storeProperties.isMetricsEnabled());
        otherProps.put("store.keep-alive-interval", storeProperties.getKeepAliveInterval() + " ms");
        otherProps.put("store.block-receive-delay-seconds", storeProperties.getBlockReceiveDelaySeconds() + " sec");
        otherProps.put("store.continue-on-parse-error", storeProperties.isContinueOnParseError());
        sections.add(ConfigSectionDto.builder()
                .name("Other Configuration")
                .properties(otherProps)
                .build());

        // Store-Specific Configuration Sections
        addStoreSection(sections, "Utxo Store", "store.utxo",
                "enabled", "api-enabled", "save-address", "pruning-enabled");

        addStoreSection(sections, "Blocks Store", "store.blocks",
                "enabled", "api-enabled", "save-cbor", "cbor-pruning-enabled", "cbor-pruning-safe-slots");

        addStoreSection(sections, "Transaction Store", "store.transaction",
                "enabled", "api-enabled", "pruning-enabled", "save-witness", "save-cbor", "cbor-pruning-enabled");

        addStoreSection(sections, "Script Store", "store.script",
                "enabled", "api-enabled");

        addStoreSection(sections, "Metadata Store", "store.metadata",
                "enabled", "api-enabled");

        addStoreSection(sections, "Assets Store", "store.assets",
                "enabled", "api-enabled");

        addStoreSection(sections, "Epoch Store", "store.epoch",
                "enabled", "api-enabled", "n2c-epoch-param-enabled", "n2c-protocol-param-fetching-interval-in-minutes");

        addStoreSection(sections, "Staking Store", "store.staking",
                "enabled", "api-enabled");

        addStoreSection(sections, "MIR Store", "store.mir",
                "enabled", "api-enabled");

        addStoreSection(sections, "Governance Store", "store.governance",
                "enabled", "api-enabled", "n2c-gov-state-enabled", "n2c-gov-state-fetching-interval-in-minutes", "n2c-drep-stake-enabled");

        addStoreSection(sections, "Submit Store", "store.submit",
                "enabled");

        addStoreSection(sections, "Admin Store", "store.admin",
                "api-enabled", "auto-recovery-enabled", "health-check-interval");

        addStoreSection(sections, "Account Store", "store.account",
                "enabled", "api-enabled", "balance-aggregation-enabled", "pruning-enabled", "history-cleanup-enabled");

        addStoreSection(sections, "Epoch Aggregation Store", "store.epoch-aggr",
                "enabled", "api-enabled", "epoch-calculation-enabled", "epoch-calculation-interval");

        addStoreSection(sections, "AdaPot Store", "store.adapot",
                "enabled", "api-enabled", "update-reward-db-batch-size");

        addStoreSection(sections, "Governance Aggregation Store", "store.governance-aggr",
                "enabled", "api-enabled");

        addStoreSection(sections, "Live Store", "store.live",
                "enabled", "api-enabled");

        return sections;
    }

    /**
     * Adds a store-specific configuration section by reading properties from Spring Environment.
     * Only adds the section if at least one property has a value set.
     */
    private void addStoreSection(List<ConfigSectionDto> sections, String sectionName,
                                  String prefix, String... propertyNames) {
        Map<String, Object> props = new LinkedHashMap<>();
        for (String propName : propertyNames) {
            String fullKey = prefix + "." + propName;
            String value = environment.getProperty(fullKey);
            if (value != null) {
                props.put(fullKey, value);
            }
        }
        if (!props.isEmpty()) {
            sections.add(ConfigSectionDto.builder()
                    .name(sectionName)
                    .properties(props)
                    .build());
        }
    }

    private String maskIfEmpty(String value) {
        return value == null || value.isEmpty() ? "(not set)" : value;
    }
}
