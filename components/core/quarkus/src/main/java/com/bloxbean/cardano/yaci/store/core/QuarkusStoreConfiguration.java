package com.bloxbean.cardano.yaci.store.core;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.GenesisBlockFinder;
import com.bloxbean.cardano.yaci.helper.TipFinder;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.executor.ParallelExecutor;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.BlockFinder;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;

@ApplicationScoped
@Slf4j
public class QuarkusStoreConfiguration {

    @Inject
    private StoreProperties storeProperties;

    @Produces
    //ConditionalOnMissingBean
    public CursorStorage cursorStorage(CursorRepository cursorRepository) {
        return new CursorStorageImpl(cursorRepository);
    }

    @Produces
    //ConditionalOnMissingBean
    public EraStorage eraStorage(EraRepository eraRepository, EraMapper eraMapper) {
        return new EraStorageImpl(eraRepository, eraMapper);
    }

//    @Bean
//    @ConditionalOnExpression("${store.cardano.cursor-no-of-blocks-to-keep:1} > 0")
//    @ReadOnly(false)
//    public CursorCleanupScheduler cursorCleanupScheduler(CursorStorage cursorStorage, StoreProperties storeProperties) {
//        log.info("<<< Enable CursorCleanupScheduler >>>");
//        log.info("CursorCleanupScheduler will run every {} sec", storeProperties.getCursorCleanupInterval());
//        log.info("CursorCleanupScheduler will keep {} blocks in cursor", storeProperties.getCursorNoOfBlocksToKeep());
//        return new CursorCleanupScheduler(cursorStorage, storeProperties);
//    }

//    @Bean
//    public LocalClientProviderManager localClientProviderManager(Environment env,
//                                                                 @Qualifier("localClientProviderPool") @Nullable GenericObjectPool<LocalClientProvider> localClientProviderPool,
//                                                                 StoreProperties storeProperties) {
//
//        if (env.containsProperty("store.cardano.n2c-node-socket-path") || env.containsProperty("store.cardano.n2c-host")) {
//            log.info("<< Initializing LocalClientProviderManager >>");
//            return new LocalClientProviderManager(localClientProviderPool, storeProperties);
//        } else {
//            return null;
//        }
//    }

    /** From spring boot starter **/
    static {
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");
    }

    //configuration

    @Produces
    public TipFinder tipFinder() {
        TipFinder tipFinder = new TipFinder(storeProperties.getCardanoHost(), storeProperties.getCardanoPort(),
                Point.ORIGIN, storeProperties.getProtocolMagic());
        tipFinder.start();
        return tipFinder;
    }

    @Produces
    @Scope("prototype")
    public BlockRangeSync blockRangeSync() {
        log.info("Creating BlockRangeSync to fetch blocks");
        BlockRangeSync blockRangeSync = new BlockRangeSync(storeProperties.getCardanoHost(),
                storeProperties.getCardanoPort(),
                storeProperties.getProtocolMagic());
        return blockRangeSync;
    }

    @Produces
    @ReadOnly(false)
    public BlockSync blockSync() {
        BlockSync blockSync = new BlockSync(storeProperties.getCardanoHost(),
                storeProperties.getCardanoPort(),
                storeProperties.getProtocolMagic(), Point.ORIGIN);
        return blockSync;
    }

    @Produces
    @ReadOnly(false)
    public GenesisBlockFinder genesisBlockFinder() {
        GenesisBlockFinder genesisBlockFinder = new GenesisBlockFinder(storeProperties.getCardanoHost(),
                storeProperties.getCardanoPort(), storeProperties.getProtocolMagic());
        return genesisBlockFinder;
    }

    @Produces
    @ReadOnly(false)
    public BlockFinder blockFinder(BlockSync blockSync) {
        BlockFinder blockFinder = new BlockFinder(blockSync);
        return blockFinder;
    }


    @Produces
    public ParallelExecutor executorHelper() {
        return new ParallelExecutor();
    }

}
