package com.bloxbean.cardano.yaci.store.starter.assetstore;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetStoreExtConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(AssetStoreExtProperties.class)
@Import({AssetStoreExtConfiguration.class})
@Slf4j
public class AssetStoreExtAutoConfiguration {

    @Autowired
    AssetStoreExtProperties properties;
}
