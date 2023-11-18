package com.bloxbean.cardano.yaci.store.starter.blocks;

import com.bloxbean.cardano.yaci.store.api.blocks.BlocksApiConfiguration;
import com.bloxbean.cardano.yaci.store.blocks.BlocksStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(BlocksStoreProperties.class)
@Import({BlocksStoreConfiguration.class, BlocksApiConfiguration.class})
@Slf4j
public class BlocksStoreAutoConfiguration {

    @Autowired
    BlocksStoreProperties properties;
}
