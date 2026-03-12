package com.bloxbean.cardano.yaci.store.blockfrost.pools;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = {"store.extensions.blockfrost.pools.enabled"},
        havingValue = "true",
        matchIfMissing = false
)
@ComponentScan(basePackages = {
        "com.bloxbean.cardano.yaci.store.blockfrost.pools",
        "com.bloxbean.cardano.yaci.store.blockfrost.common",
})
public class BFPoolsConfiguration {

}
