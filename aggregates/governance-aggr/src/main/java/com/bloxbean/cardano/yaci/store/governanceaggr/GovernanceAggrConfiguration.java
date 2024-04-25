package com.bloxbean.cardano.yaci.store.governanceaggr;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.governance-aggr",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.governanceaggr"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.governanceaggr"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.governanceaggr"})
@EnableTransactionManagement
@EnableScheduling
public class GovernanceAggrConfiguration {

}
