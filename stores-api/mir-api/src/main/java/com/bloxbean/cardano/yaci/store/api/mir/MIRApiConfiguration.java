package com.bloxbean.cardano.yaci.store.api.mir;

import com.bloxbean.cardano.yaci.store.api.mir.storage.MIRReader;
import com.bloxbean.cardano.yaci.store.api.mir.storage.impl.MIRReadRepository;
import com.bloxbean.cardano.yaci.store.api.mir.storage.impl.MIRReaderImpl;
import com.bloxbean.cardano.yaci.store.api.mir.storage.impl.MIRReaderMapper;
import com.bloxbean.cardano.yaci.store.mir.MIRStoreProperties;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.jpa.mapper.MIRMapper;
import org.springframework.beans.factory.annotation.Qualifier;
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
        name = "api-enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.mir"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.api.mir.storage"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.mir.storage"})
@EnableTransactionManagement
@EnableConfigurationProperties(MIRStoreProperties.class)
public class MIRApiConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MIRReader mirReader(@Qualifier("mirReadRepository") MIRReadRepository mirReadRepository,
                               MIRReaderMapper readerMapper,
                               MIRMapper mapper) {
        return new MIRReaderImpl(mirReadRepository, readerMapper, mapper);
    }

}
