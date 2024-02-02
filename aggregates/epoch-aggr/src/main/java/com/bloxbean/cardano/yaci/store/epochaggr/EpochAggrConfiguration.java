package com.bloxbean.cardano.yaci.store.epochaggr;

import com.bloxbean.cardano.yaci.store.epochaggr.storage.EpochStorage;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.EpochStorageReader;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.EpochStorageImpl;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.EpochStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.mapper.EpochMapper;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.repository.EpochRepository;
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
        prefix = "store.epoch-aggr",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.epochaggr"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.epochaggr"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.epochaggr"})
@EnableTransactionManagement
@EnableScheduling
public class EpochAggrConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EpochStorage epochStorage(EpochRepository epochRepository, EpochMapper epochMapper) {
        return new EpochStorageImpl(epochRepository, epochMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EpochStorageReader epochStorageReader(EpochRepository epochReadRepository, EpochMapper epochMapper) {
        return new EpochStorageReaderImpl(epochReadRepository, epochMapper);
    }
}
