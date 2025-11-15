package com.bloxbean.cardano.yaci.store.starter.governanceaggr;

import com.bloxbean.cardano.yaci.store.api.governanceaggr.GovernanceAggrApiConfiguration;
import com.bloxbean.cardano.yaci.store.governanceaggr.GovernanceAggrConfiguration;
import com.bloxbean.cardano.yaci.store.governanceaggr.GovernanceAggrProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(GovernanceAggrAutoConfigProperties.class)
@Import({GovernanceAggrConfiguration.class, GovernanceAggrApiConfiguration.class})
@Slf4j
public class GovernanceAggrStoreAutoConfiguration {

    @Autowired
    GovernanceAggrAutoConfigProperties properties;

    @Bean
    public GovernanceAggrProperties governanceAggrProperties() {
        GovernanceAggrProperties governanceAggrProperties = new GovernanceAggrProperties();
        governanceAggrProperties.setEnabled(properties.getGovernanceAggr().isEnabled());
        governanceAggrProperties.setDevnetConwayBootstrapAvailable(properties.getGovernanceAggr().isDevnetConwayBootstrapAvailable());

        governanceAggrProperties.setDrepDistWorkMem(properties.getGovernanceAggr().getDrepDistWorkMem());

        return governanceAggrProperties;
    }
}
