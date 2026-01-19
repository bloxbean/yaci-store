package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.store.adminui.dto.ConfigSectionDto;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public List<ConfigSectionDto> getConfiguration() {
        List<ConfigSectionDto> sections = new ArrayList<>();

        // Cardano Network Section
        Map<String, Object> cardanoProps = new LinkedHashMap<>();
        cardanoProps.put("host", maskIfEmpty(storeProperties.getCardanoHost()));
        cardanoProps.put("port", storeProperties.getCardanoPort());
        cardanoProps.put("protocolMagic", storeProperties.getProtocolMagic());
        cardanoProps.put("n2cNodeSocketPath", maskIfEmpty(storeProperties.getN2cNodeSocketPath()));
        cardanoProps.put("n2cHost", maskIfEmpty(storeProperties.getN2cHost()));
        cardanoProps.put("n2cPort", storeProperties.getN2cPort());
        cardanoProps.put("isMainnet", storeProperties.isMainnet());
        sections.add(ConfigSectionDto.builder()
                .name("Cardano Network")
                .properties(cardanoProps)
                .build());

        // Sync Configuration Section
        Map<String, Object> syncProps = new LinkedHashMap<>();
        syncProps.put("syncAutoStart", storeProperties.isSyncAutoStart());
        syncProps.put("syncStartSlot", storeProperties.getSyncStartSlot());
        syncProps.put("syncStartBlockhash", maskIfEmpty(storeProperties.getSyncStartBlockhash()));
        syncProps.put("syncStopSlot", storeProperties.getSyncStopSlot());
        syncProps.put("syncStopBlockhash", maskIfEmpty(storeProperties.getSyncStopBlockhash()));
        syncProps.put("blockDiffToStartSyncProtocol", storeProperties.getBlockDiffToStartSyncProtocol());
        sections.add(ConfigSectionDto.builder()
                .name("Sync Configuration")
                .properties(syncProps)
                .build());

        // Executor Configuration Section
        Map<String, Object> executorProps = new LinkedHashMap<>();
        executorProps.put("blockProcessingThreads", storeProperties.getBlockProcessingThreads());
        executorProps.put("eventProcessingThreads", storeProperties.getEventProcessingThreads());
        executorProps.put("enableParallelProcessing", storeProperties.isEnableParallelProcessing());
        executorProps.put("blocksBatchSize", storeProperties.getBlocksBatchSize());
        executorProps.put("blocksPartitionSize", storeProperties.getBlocksPartitionSize());
        executorProps.put("useVirtualThreadForBatchProcessing", storeProperties.isUseVirtualThreadForBatchProcessing());
        executorProps.put("useVirtualThreadForEventProcessing", storeProperties.isUseVirtualThreadForEventProcessing());
        executorProps.put("processingThreadsTimeout", storeProperties.getProcessingThreadsTimeout() + " min");
        sections.add(ConfigSectionDto.builder()
                .name("Executor Configuration")
                .properties(executorProps)
                .build());

        // Database Configuration Section
        Map<String, Object> dbProps = new LinkedHashMap<>();
        dbProps.put("dbBatchSize", storeProperties.getDbBatchSize());
        dbProps.put("dbParallelInsert", storeProperties.isDbParallelInsert());
        dbProps.put("parallelWrite", storeProperties.isParallelWrite());
        dbProps.put("writeThreadCount", storeProperties.getWriteThreadCount());
        dbProps.put("writeThreadDefaultBatchSize", storeProperties.getWriteThreadDefaultBatchSize());
        dbProps.put("jooqWriteBatchSize", storeProperties.getJooqWriteBatchSize());
        sections.add(ConfigSectionDto.builder()
                .name("Database Configuration")
                .properties(dbProps)
                .build());

        // Cursor Configuration Section
        Map<String, Object> cursorProps = new LinkedHashMap<>();
        cursorProps.put("cursorNoOfBlocksToKeep", storeProperties.getCursorNoOfBlocksToKeep());
        cursorProps.put("cursorCleanupInterval", storeProperties.getCursorCleanupInterval() + " sec");
        sections.add(ConfigSectionDto.builder()
                .name("Cursor Configuration")
                .properties(cursorProps)
                .build());

        // N2C Pool Configuration Section
        Map<String, Object> n2cPoolProps = new LinkedHashMap<>();
        n2cPoolProps.put("n2cPoolEnabled", storeProperties.isN2cPoolEnabled());
        n2cPoolProps.put("n2cPoolMaxTotal", storeProperties.getN2cPoolMaxTotal());
        n2cPoolProps.put("n2cPoolMinIdle", storeProperties.getN2cPoolMinIdle());
        n2cPoolProps.put("n2cPoolMaxIdle", storeProperties.getN2cPoolMaxIdle());
        n2cPoolProps.put("n2cPoolMaxWaitInMillis", storeProperties.getN2cPoolMaxWaitInMillis() + " ms");
        sections.add(ConfigSectionDto.builder()
                .name("N2C Pool Configuration")
                .properties(n2cPoolProps)
                .build());

        // Plugin Configuration Section
        Map<String, Object> pluginProps = new LinkedHashMap<>();
        pluginProps.put("pluginsEnabled", storeProperties.isPluginsEnabled());
        pluginProps.put("pluginExitOnError", storeProperties.isPluginExitOnError());
        pluginProps.put("pluginMetricsEnabled", storeProperties.isPluginMetricsEnabled());
        pluginProps.put("pluginApiEnabled", storeProperties.isPluginApiEnabled());
        sections.add(ConfigSectionDto.builder()
                .name("Plugin Configuration")
                .properties(pluginProps)
                .build());

        // Other Configuration Section
        Map<String, Object> otherProps = new LinkedHashMap<>();
        otherProps.put("readOnlyMode", storeProperties.isReadOnlyMode());
        otherProps.put("primaryInstance", storeProperties.isPrimaryInstance());
        otherProps.put("metricsEnabled", storeProperties.isMetricsEnabled());
        otherProps.put("keepAliveInterval", storeProperties.getKeepAliveInterval() + " ms");
        otherProps.put("blockReceiveDelaySeconds", storeProperties.getBlockReceiveDelaySeconds() + " sec");
        otherProps.put("continueOnParseError", storeProperties.isContinueOnParseError());
        sections.add(ConfigSectionDto.builder()
                .name("Other Configuration")
                .properties(otherProps)
                .build());

        return sections;
    }

    private String maskIfEmpty(String value) {
        return value == null || value.isEmpty() ? "(not set)" : value;
    }
}
