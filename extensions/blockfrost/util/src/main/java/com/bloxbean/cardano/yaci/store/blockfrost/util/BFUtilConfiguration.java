package com.bloxbean.cardano.yaci.store.blockfrost.util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = "store.extensions.blockfrost.util.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = "com.bloxbean.cardano.yaci.store.blockfrost.util")
public class BFUtilConfiguration {
}
