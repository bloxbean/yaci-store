package com.bloxbean.cardano.yaci.store.live;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.live",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.live"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.live"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.live"})
@EnableTransactionManagement
public class LiveStoreConfiguration {
}
