package com.bloxbean.cardano.yaci.store.api.governanceaggr;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConditionalOnProperty(name = {"store.governance-aggr.enabled", "store.governance-aggr.api-enabled"},
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.governanceaggr"})
@EnableScheduling
public class GovernanceAggrApiConfiguration {

}
