package com.bloxbean.cardano.yaci.store.blockfrost.block;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = {"store.extensions.blockfrost.blocks.enabled"},
        havingValue = "true",
        matchIfMissing = false
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.blockfrost.block"})
public class BFBlockConfiguration {
}

