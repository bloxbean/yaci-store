package com.bloxbean.cardano.yaci.store.core;

import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.CursorCleanupScheduler;
import com.bloxbean.cardano.yaci.store.core.service.local.LocalClientProviderManager;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.*;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.core",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.core", "com.bloxbean.cardano.yaci.store.dbutils"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.core"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.core"})
@EnableTransactionManagement
@EnableScheduling
@Slf4j
public class StoreConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public CursorStorage cursorStorage(CursorRepository cursorRepository) {
        return new CursorStorageImpl(cursorRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public EraStorage eraStorage(EraRepository eraRepository, EraMapper eraMapper) {
        return new EraStorageImpl(eraRepository, eraMapper);
    }

    @Bean
    @ConditionalOnExpression("${store.cardano.cursor-no-of-blocks-to-keep:1} > 0")
    @ReadOnly(false)
    public CursorCleanupScheduler cursorCleanupScheduler(CursorStorage cursorStorage, StoreProperties storeProperties) {
        log.info("<<< Enable CursorCleanupScheduler >>>");
        log.info("CursorCleanupScheduler will run every {} sec", storeProperties.getCursorCleanupInterval());
        log.info("CursorCleanupScheduler will keep {} blocks in cursor", storeProperties.getCursorNoOfBlocksToKeep());
        return new CursorCleanupScheduler(cursorStorage, storeProperties);
    }

    @Bean
    public LocalClientProviderManager localClientProviderManager(Environment env,
                                                                 @Qualifier("localClientProviderPool") @Nullable GenericObjectPool<LocalClientProvider> localClientProviderPool,
                                                                 StoreProperties storeProperties) {

        if (env.containsProperty("store.cardano.n2c-node-socket-path") || env.containsProperty("store.cardano.n2c-host")) {
            log.info("<< Initializing LocalClientProviderManager >>");
            return new LocalClientProviderManager(localClientProviderPool, storeProperties);
        } else {
            return null;
        }
    }

}
