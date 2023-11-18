package com.bloxbean.cardano.yaci.store.mir;

import com.bloxbean.cardano.yaci.store.mir.storage.MIRStorage;
import com.bloxbean.cardano.yaci.store.mir.storage.MIRStorageReader;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.MIRRepository;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.MIRStorageImpl;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.MIRStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.mapper.MIRMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.mir",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.mir"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.mir"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.mir"})
@EnableTransactionManagement
@EnableConfigurationProperties(MIRStoreProperties.class)
public class MIRStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MIRStorage mirStorage(MIRRepository mirRepository,
                                     MIRMapper mapper) {
        return new MIRStorageImpl(mirRepository, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public MIRStorageReader mirStorageReader(MIRRepository mirRepository,
                                       MIRMapper mapper) {
        return new MIRStorageReaderImpl(mirRepository, mapper);
    }

}
