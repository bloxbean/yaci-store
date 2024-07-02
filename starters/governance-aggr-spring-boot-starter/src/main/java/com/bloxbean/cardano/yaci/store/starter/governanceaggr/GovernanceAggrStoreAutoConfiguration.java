package com.bloxbean.cardano.yaci.store.starter.governanceaggr;

import com.bloxbean.cardano.yaci.store.governanceaggr.GovernanceAggrConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(GovernanceAggrStoreAutoConfigProperties.class)
@Import({GovernanceAggrConfiguration.class, GovernanceAggrConfiguration.class})
@Slf4j
public class GovernanceAggrStoreAutoConfiguration {

}
