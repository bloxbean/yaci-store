package com.bloxbean.cardano.yaci.store.remote.publisher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.remote",
        name = "publisher-enabled",
        havingValue = "true",
        matchIfMissing = false
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.remote.publisher"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.remote.publisher"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.remote.publisher"})
@EnableTransactionManagement
public class RemotePublisherConfiguration {
}
