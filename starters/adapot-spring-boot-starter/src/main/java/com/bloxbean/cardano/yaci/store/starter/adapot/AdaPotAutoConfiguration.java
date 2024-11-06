package com.bloxbean.cardano.yaci.store.starter.adapot;

import com.bloxbean.cardano.yaci.store.adapot.AdaPotConfiguration;
import com.bloxbean.cardano.yaci.store.adapot.AdaPotProperties;
import com.bloxbean.cardano.yaci.store.api.adapot.AdaPotApiConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(AdaPotAutoConfigProperties.class)
@Import({AdaPotConfiguration.class, AdaPotApiConfiguration.class})
@Slf4j
public class AdaPotAutoConfiguration {

    @Autowired
    AdaPotAutoConfigProperties properties;

    @Bean
    public AdaPotProperties adaPotProperties() {
        AdaPotProperties adaPotProperties = new AdaPotProperties();
        adaPotProperties.setUpdateRewardDbBatchSize(properties.getAdaPot().getUpdateRewardDbBatchSize());

        return adaPotProperties;
    }
}
