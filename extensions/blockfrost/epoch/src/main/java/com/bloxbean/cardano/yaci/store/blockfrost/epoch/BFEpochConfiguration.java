package com.bloxbean.cardano.yaci.store.blockfrost.epoch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = {"store.extensions.blockfrost.epoch.enabled"},
        havingValue = "true",
        matchIfMissing = false
)
@ComponentScan(basePackages = {
        "com.bloxbean.cardano.yaci.store.blockfrost.epoch",
})
public class BFEpochConfiguration {

}
