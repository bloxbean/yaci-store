package com.bloxbean.cardano.yaci.store.remote.consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.remote",
        name = "consumer-enabled",
        havingValue = "true",
        matchIfMissing = false
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.remote.consumer"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.remote.consumer"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.remote.consumer"})
@EnableTransactionManagement
public class RemoteConsumerConfiguration {

}
