package com.bloxbean.cardano.yaci.store.starter.blockfrost;

import com.bloxbean.cardano.yaci.store.blockfrost.epoch.BFEpochConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(BFProperties.class)
@Import(BFEpochConfiguration.class)
@Slf4j
public class BFAutoConfiguration {

    @Autowired
    BFProperties properties;

}
