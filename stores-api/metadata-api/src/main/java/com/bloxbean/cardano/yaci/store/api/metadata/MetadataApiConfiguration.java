package com.bloxbean.cardano.yaci.store.api.metadata;

import com.bloxbean.cardano.yaci.store.api.metadata.storage.TxMetadataReader;
import com.bloxbean.cardano.yaci.store.api.metadata.storage.impl.TxMetadataLabelReadRepository;
import com.bloxbean.cardano.yaci.store.api.metadata.storage.impl.TxMetadataReaderImpl;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.jpa.MetadataMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.metadata",
        name = "api-enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.metadata"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.api.metadata.storage"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.metadata.storage"})
@EnableTransactionManagement
public class MetadataApiConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TxMetadataReader txMetadataReader(TxMetadataLabelReadRepository txMetadataLabelReadRepository,
                                             MetadataMapper metadataMapper) {
        return new TxMetadataReaderImpl(txMetadataLabelReadRepository, metadataMapper);
    }
}
