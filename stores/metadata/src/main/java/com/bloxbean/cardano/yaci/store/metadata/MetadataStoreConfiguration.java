package com.bloxbean.cardano.yaci.store.metadata;

import com.bloxbean.cardano.yaci.store.metadata.storage.TxMetadataStorage;
import com.bloxbean.cardano.yaci.store.metadata.storage.TxMetadataStorageReader;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.TxMetadataStorageImpl;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.TxMetadataStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.mapper.MetadataMapper;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.repository.TxMetadataLabelRepository;
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
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.metadata", "com.bloxbean.cardano.yaci.store.vertx", "com.bloxbean.cardano.yaci.store.filter"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.metadata"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.metadata"})
@EnableTransactionManagement
public class MetadataStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TxMetadataStorage txMetadataStorage(TxMetadataLabelRepository txMetadataLabelRepository,
                                               MetadataMapper metadataMapper) {
        return new TxMetadataStorageImpl(txMetadataLabelRepository, metadataMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public TxMetadataStorageReader txMetadataStorageReader(TxMetadataLabelRepository txMetadataLabelReadRepository,
                                                    MetadataMapper metadataMapper) {
        return new TxMetadataStorageReaderImpl(txMetadataLabelReadRepository, metadataMapper);
    }
}
