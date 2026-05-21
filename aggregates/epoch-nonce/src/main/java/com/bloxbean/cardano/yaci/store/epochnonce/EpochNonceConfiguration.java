package com.bloxbean.cardano.yaci.store.epochnonce;

import com.bloxbean.cardano.yaci.store.epochnonce.storage.EpochNonceStorage;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.EpochNonceStorageReader;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.impl.EpochNonceStorageImpl;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.impl.EpochNonceStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.impl.mapper.EpochNonceMapper;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.impl.repository.EpochNonceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@AutoConfiguration
@ConditionalOnProperty(
        prefix = "store.epoch-nonce",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.epochnonce"})
@EnableJpaRepositories(basePackages = {"com.bloxbean.cardano.yaci.store.epochnonce"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.epochnonce"})
@EnableTransactionManagement
public class EpochNonceConfiguration {
    public static final String STORE_EPOCH_NONCE_ENABLED = "store.epoch-nonce.enabled";

    @Bean
    @ConditionalOnMissingBean
    public EpochNonceStorage epochNonceStorage(EpochNonceRepository epochNonceRepository, EpochNonceMapper epochNonceMapper) {
        return new EpochNonceStorageImpl(epochNonceRepository, epochNonceMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EpochNonceStorageReader epochNonceStorageReader(EpochNonceRepository epochNonceRepository, EpochNonceMapper epochNonceMapper) {
        return new EpochNonceStorageReaderImpl(epochNonceRepository, epochNonceMapper);
    }
}
