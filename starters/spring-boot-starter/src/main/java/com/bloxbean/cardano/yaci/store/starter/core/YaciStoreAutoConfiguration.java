package com.bloxbean.cardano.yaci.store.starter.core;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.*;
import com.bloxbean.cardano.yaci.store.client.utxo.DummyUtxoClient;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClientImpl;
import com.bloxbean.cardano.yaci.store.common.executor.ParallelExecutor;
import com.bloxbean.cardano.yaci.store.core.StoreConfiguration;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.service.ApplicationStartListener;
import com.bloxbean.cardano.yaci.store.core.service.BlockFinder;
import lombok.extern.slf4j.Slf4j;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

@AutoConfiguration
@EnableConfigurationProperties(YaciStoreProperties.class)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.common", "com.bloxbean.cardano.yaci.store.events"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.common", "com.bloxbean.cardano.yaci.store.events"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.common", "com.bloxbean.cardano.yaci.store.events"})
@EnableTransactionManagement
@EnableScheduling
@Import(StoreConfiguration.class)
@Slf4j
public class YaciStoreAutoConfiguration {
    @Autowired
    YaciStoreProperties properties;

    static {
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");
    }

    //configuration

    @Bean
    public TipFinder tipFinder() {
        TipFinder tipFinder = new TipFinder(properties.getCardano().getHost(), properties.getCardano().getPort(),
                Point.ORIGIN, properties.getCardano().getProtocolMagic());
        tipFinder.start();
        return tipFinder;
    }

    @Bean
    @Scope("prototype")
    public BlockRangeSync blockRangeSync() {
        log.info("Creating BlockRangeSync to fetch blocks");
        BlockRangeSync blockRangeSync = new BlockRangeSync(properties.getCardano().getHost(), properties.getCardano().getPort(), properties.getCardano().getProtocolMagic());
        return blockRangeSync;
    }

    @Bean
    public BlockSync blockSync() {
        BlockSync blockSync = new BlockSync(properties.getCardano().getHost(), properties.getCardano().getPort(), properties.getCardano().getProtocolMagic(), Point.ORIGIN);
        return blockSync;
    }

    @Bean
    public GenesisBlockFinder genesisBlockFinder() {
        GenesisBlockFinder genesisBlockFinder = new GenesisBlockFinder(properties.getCardano().getHost(), properties.getCardano().getPort(), properties.getCardano().getProtocolMagic());
        return genesisBlockFinder;
    }

    @Bean
    public BlockFinder blockFinder(BlockSync blockSync) {
        BlockFinder blockFinder = new BlockFinder(blockSync);
        return blockFinder;
    }

    @Bean(name = "localClientProvider")
    @ConditionalOnProperty(prefix = "store.cardano", name = "n2c-node-socket-path")
    @Primary
    public LocalClientProvider localClientProviderNodeSocketPath() {
        log.info("LocalStateQueryClient ---> Configured --> " + properties.getCardano().getN2cNodeSocketPath());
        return new LocalClientProvider(properties.getCardano().getN2cNodeSocketPath(), properties.getCardano().getProtocolMagic());
    }

    @Bean(name = "localClientProvider")
    @ConditionalOnProperty(prefix = "store.cardano", name = "n2c-host")
    public LocalClientProvider localClientProviderNodeSocketPort() {
        if (properties.getCardano().getN2cPort() == 0)
            throw new IllegalArgumentException("Invalid cardano.n2c.port " + properties.getCardano().getN2cPort() );
        log.info("LocalStateQueryClient ---> Configured (n2c host/port)--> " + properties.getCardano().getN2cHost() + ", " + properties.getCardano().getN2cPort());
        return new LocalClientProvider(properties.getCardano().getN2cHost(), properties.getCardano().getN2cPort(), properties.getCardano().getProtocolMagic());

    }

    @Bean
    @ConditionalOnBean(name = {"localClientProvider"})
    public ApplicationStartListener applicationStartListener(LocalClientProvider localClientProvider) {
        log.info("ApplicationStartListener with LocalClientProvider created >>");
        return new ApplicationStartListener(localClientProvider);
    }

    @Bean
    @ConditionalOnMissingBean(name = "utxoClient")
    public UtxoClient utxoClient(RestTemplate restTemplate) {
        if (properties.getUtxoClientUrl() != null && !properties.getUtxoClientUrl().isEmpty())
            return new UtxoClientImpl(restTemplate, properties.getUtxoClientUrl().trim());
        else
            return new DummyUtxoClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public DefaultConfigurationCustomizer configurationCustomiser() {
        return (DefaultConfiguration c) -> c.settings()
                .withRenderQuotedNames(
                        RenderQuotedNames.NEVER
                );
    }

    @Bean
    public ParallelExecutor executorHelper() {
        return new ParallelExecutor();
    }

    @Bean
    public StoreProperties storeProperties() {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setEventPublisherId(properties.getEventPublisherId());
        storeProperties.setSyncAutoStart(properties.isSyncAutoStart());

        storeProperties.setCardanoHost(properties.getCardano().getHost());
        storeProperties.setCardanoPort(properties.getCardano().getPort());
        storeProperties.setProtocolMagic(properties.getCardano().getProtocolMagic());
        storeProperties.setN2cNodeSocketPath(properties.getCardano().getN2cNodeSocketPath());
        storeProperties.setN2cHost(properties.getCardano().getN2cHost());
        storeProperties.setN2cPort(properties.getCardano().getN2cPort());
        storeProperties.setSubmitApiUrl(properties.getCardano().getSubmitApiUrl());
        storeProperties.setOgmiosUrl(properties.getCardano().getOgmiosUrl());
        storeProperties.setMempoolMonitoringEnabled(properties.getCardano().getMempoolMonitoringEnabled());

        storeProperties.setShelleyStartSlot(properties.getCardano().getShelleyStartSlot());
        storeProperties.setShelleyStartBlockhash(properties.getCardano().getShelleyStartBlockhash());
        storeProperties.setShelleyStartBlock(properties.getCardano().getShelleyStartBlock());

        storeProperties.setSyncStartSlot(properties.getCardano().getSyncStartSlot());
        storeProperties.setSyncStartBlockhash(properties.getCardano().getSyncStartBlockhash());
        storeProperties.setSyncStartByronBlockNumber(properties.getCardano().getSyncStartByronBlockNumber());

        storeProperties.setSyncStopSlot(properties.getCardano().getSyncStopSlot());
        storeProperties.setSyncStopBlockhash(properties.getCardano().getSyncStopBlockhash());

        storeProperties.setByronGenesisFile(properties.getCardano().getByronGenesisFile());
        storeProperties.setShelleyGenesisFile(properties.getCardano().getShelleyGenesisFile());
        storeProperties.setAlonzoGenesisFile(properties.getCardano().getAlonzoGenesisFile());
        storeProperties.setConwayGenesisFile(properties.getCardano().getConwayGenesisFile());

        storeProperties.setBlockDiffToStartSyncProtocol(properties.getCardano().getBlockDiffToStartSyncProtocol());
        storeProperties.setCursorNoOfBlocksToKeep(properties.getCardano().getCursorNoOfBlocksToKeep());
        storeProperties.setCursorCleanupInterval(properties.getCardano().getCursorCleanupInterval());

        storeProperties.setKeepAliveInterval(properties.getCardano().getKeepAliveInterval());

        storeProperties.setDefaultGenesisHash(properties.getCardano().getDefaultGenesisHash());

        //executor properties
        storeProperties.setEnableParallelProcessing(properties.getExecutor().isEnableParallelProcessing());
        storeProperties.setBlockProcessingThreads(properties.getExecutor().getBlockProcessingThreads());
        storeProperties.setEventProcessingThreads(properties.getExecutor().getEventProcessingThreads());

        storeProperties.setBlocksBatchSize(properties.getExecutor().getBlocksBatchSize());
        storeProperties.setBlocksPartitionSize(properties.getExecutor().getBlocksPartitionSize());

        storeProperties.setUseVirtualThreadForBatchProcessing(properties.getExecutor().isUseVirtualThreadForBatchProcessing());
        storeProperties.setUseVirtualThreadForEventProcessing(properties.getExecutor().isUseVirtualThreadForEventProcessing());

        storeProperties.setProcessingThreadsTimeout(properties.getExecutor().getProcessingThreadsTimeout());

        storeProperties.setDbBatchSize(properties.getDb().getBatchSize());
        storeProperties.setDbParallelInsert(properties.getDb().isParallelInsert());

        storeProperties.setParallelWrite(properties.getDb().isParallelWrite());
        storeProperties.setWriteThreadDefaultBatchSize(properties.getDb().getWriteThreadDefaultBatchSize());
        storeProperties.setJooqWriteBatchSize(properties.getDb().getJooqWriteBatchSize());
        storeProperties.setWriteThreadCount(properties.getDb().getWriteThreadCount());

        storeProperties.setMvstoreEnabled(properties.isMvstoreEnabled());
        storeProperties.setMvstorePath(properties.getMvstorePath());

        return storeProperties;
    }

}
