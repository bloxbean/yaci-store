package com.bloxbean.cardano.yaci.store.metadata;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
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
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.metadata"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.metadata"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.metadata"})
@EnableTransactionManagement
public class MetadataStoreConfiguration {
}
