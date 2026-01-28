package com.bloxbean.cardano.yaci.store.blockfrost.epoch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = {"store.extensions.blockfrost.epoch.enabled"},
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {
        "com.bloxbean.cardano.yaci.store.blockfrost.epoch",
})
@EnableJpaRepositories(basePackages = {"com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.impl.repository"})
@EntityScan(basePackages = {
        "com.bloxbean.cardano.yaci.store.epochaggr",
        "com.bloxbean.cardano.yaci.store.blocks",
        "com.bloxbean.cardano.yaci.store.adapot"
})
public class BFEpochConfiguration {

}
