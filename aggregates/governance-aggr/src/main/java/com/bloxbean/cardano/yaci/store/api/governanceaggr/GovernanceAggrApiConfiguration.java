package com.bloxbean.cardano.yaci.store.api.governanceaggr;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = {"store.governance-aggr.enabled", "store.governance-aggr.api-enabled"},
        havingValue = "true"
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.governanceaggr"})
public class GovernanceAggrApiConfiguration {

}
