package com.bloxbean.cardano.yaci.store.starter.adapot;

import com.bloxbean.cardano.yaci.store.adapot.AdaPotStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(AdaPotStoreProperties.class)
@Import(AdaPotStoreConfiguration.class)
@Slf4j
public class AdaPotStoreAutoConfiguration {

    @Autowired
    AdaPotStoreProperties properties;
}
