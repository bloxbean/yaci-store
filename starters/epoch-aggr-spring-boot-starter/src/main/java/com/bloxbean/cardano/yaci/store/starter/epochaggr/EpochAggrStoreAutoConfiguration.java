package com.bloxbean.cardano.yaci.store.starter.epochaggr;

import com.bloxbean.cardano.yaci.store.api.epochaggr.EpochApiConfiguration;
import com.bloxbean.cardano.yaci.store.epochaggr.EpochAggrConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(EpochAggrStoreAutoConfigProperties.class)
@Import({EpochAggrConfiguration.class, EpochApiConfiguration.class})
@Slf4j
public class EpochAggrStoreAutoConfiguration {

    @Autowired
    EpochAggrStoreAutoConfigProperties properties;

}
