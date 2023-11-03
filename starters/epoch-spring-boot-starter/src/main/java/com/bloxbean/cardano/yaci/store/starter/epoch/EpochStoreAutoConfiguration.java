package com.bloxbean.cardano.yaci.store.starter.epoch;

import com.bloxbean.cardano.yaci.store.epoch.EpochStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(EpochStoreProperties.class)
@Import(EpochStoreConfiguration.class)
@Slf4j
public class EpochStoreAutoConfiguration {

    @Autowired
    EpochStoreProperties properties;
}
