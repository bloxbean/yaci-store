package com.bloxbean.cardano.yaci.store.starter.staking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(StakingStoreProperties.class)
@Import(StakingStoreConfiguration.class)
@Slf4j
public class StakingStoreAutoConfiguration {

    @Autowired
    StakingStoreProperties properties;
}
