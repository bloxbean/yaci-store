package com.bloxbean.cardano.yaci.store.starter.live;

import com.bloxbean.cardano.yaci.store.live.LiveStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(LiveStoreProperties.class)
@Import(LiveStoreConfiguration.class)
@Slf4j
public class LiveStoreAutoConfiguration {

    @Autowired
    LiveStoreProperties properties;
}
