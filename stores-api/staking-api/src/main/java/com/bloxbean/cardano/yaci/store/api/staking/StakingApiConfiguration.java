package com.bloxbean.cardano.yaci.store.api.staking;

import com.bloxbean.cardano.yaci.store.staking.StakingStoreProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "store.staking",
        name = "api-enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.staking"})
@EnableConfigurationProperties(StakingStoreProperties.class)
public class StakingApiConfiguration {

}
