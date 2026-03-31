package com.bloxbean.cardano.yaci.store.starter.assetstore;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(AssetsExtProperties.class)
@Import({AssetsExtConfiguration.class})
@Slf4j
public class AssetsExtAutoConfiguration {

    @Autowired
    AssetsExtProperties properties;
}
