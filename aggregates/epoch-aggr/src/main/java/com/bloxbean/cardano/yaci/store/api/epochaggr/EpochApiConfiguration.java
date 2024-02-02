package com.bloxbean.cardano.yaci.store.api.epochaggr;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConditionalOnProperty(name = {"store.epoch-aggr.enabled", "store.epoch-aggr.api-enabled"},
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.epochaggr"})
@EnableScheduling
public class EpochApiConfiguration {

}
