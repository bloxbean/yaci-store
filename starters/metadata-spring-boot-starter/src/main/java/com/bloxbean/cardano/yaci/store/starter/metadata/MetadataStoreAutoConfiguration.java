package com.bloxbean.cardano.yaci.store.starter.metadata;

import com.bloxbean.cardano.yaci.store.metadata.MetadataStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(MetadataStoreProperties.class)
@Import(MetadataStoreConfiguration.class)
@Slf4j
public class MetadataStoreAutoConfiguration {

    @Autowired
    MetadataStoreProperties properties;
}
