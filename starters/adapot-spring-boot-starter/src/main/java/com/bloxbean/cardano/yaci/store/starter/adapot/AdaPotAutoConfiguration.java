package com.bloxbean.cardano.yaci.store.starter.adapot;

import com.bloxbean.cardano.yaci.store.adapot.AdaPotConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(AdaPotAutoConfigProperties.class)
@Import({AdaPotConfiguration.class})
@Slf4j
public class AdaPotAutoConfiguration {

    @Autowired
    AdaPotAutoConfigProperties properties;

}
