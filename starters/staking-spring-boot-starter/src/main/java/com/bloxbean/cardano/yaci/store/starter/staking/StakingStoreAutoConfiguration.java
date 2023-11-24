package com.bloxbean.cardano.yaci.store.starter.staking;

import com.bloxbean.cardano.yaci.store.api.staking.StakingApiConfiguration;
import com.bloxbean.cardano.yaci.store.staking.StakingStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(StakingStoreProperties.class)
@Import({StakingStoreConfiguration.class, StakingApiConfiguration.class})
@Slf4j
public class StakingStoreAutoConfiguration {

    @Autowired
    StakingStoreProperties properties;
}
