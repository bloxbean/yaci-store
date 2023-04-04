package com.bloxbean.cardano.yaci.store.starter.assets;

import com.bloxbean.cardano.yaci.store.assets.AssetsStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(AssetsStoreProperties.class)
@Import(AssetsStoreConfiguration.class)
@Slf4j
public class AssetsStoreAutoConfiguration {

    @Autowired
    AssetsStoreProperties properties;
}
