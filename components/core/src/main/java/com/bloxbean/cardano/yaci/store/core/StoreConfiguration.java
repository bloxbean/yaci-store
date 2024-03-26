package com.bloxbean.cardano.yaci.store.core;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.service.CursorCleanupScheduler;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@Configuration
@EnableScheduling
@EnableTransactionManagement
@EntityScan(basePackages = "com.bloxbean.cardano.yaci.store.core")
@ComponentScan(basePackages = "com.bloxbean.cardano.yaci.store.core")
@EnableJpaRepositories(basePackages = "com.bloxbean.cardano.yaci.store.core")
@ConditionalOnProperty(prefix = "store.core", name = "enabled", havingValue = "true", matchIfMissing = true)
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
    public CursorCleanupScheduler cursorCleanupScheduler(CursorStorage cursorStorage, StoreProperties storeProperties) {
        log.info("<<< Enable CursorCleanupScheduler >>>");
        log.info("CursorCleanupScheduler will run every {} sec", storeProperties.getCursorCleanupInterval());
        log.info("CursorCleanupScheduler will keep {} blocks in cursor", storeProperties.getCursorNoOfBlocksToKeep());
        return new CursorCleanupScheduler(cursorStorage, storeProperties);
    }
}
