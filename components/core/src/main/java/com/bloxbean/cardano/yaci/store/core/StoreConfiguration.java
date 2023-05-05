package com.bloxbean.cardano.yaci.store.core;

import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
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
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.core"})
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
}
