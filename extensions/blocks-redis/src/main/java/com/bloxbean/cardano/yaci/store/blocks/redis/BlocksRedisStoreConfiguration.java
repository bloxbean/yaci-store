package com.bloxbean.cardano.yaci.store.blocks.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.blocks.redis",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.blocks.redis"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.blocks.redis"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.blocks.redis"})
@EnableTransactionManagement
@EnableScheduling
public class BlocksRedisStoreConfiguration {

    //TODO: Storage beans with @ConditionalOnMissingBean

}
