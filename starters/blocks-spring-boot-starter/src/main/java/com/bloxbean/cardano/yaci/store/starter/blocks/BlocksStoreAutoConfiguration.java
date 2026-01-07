package com.bloxbean.cardano.yaci.store.starter.blocks;

import com.bloxbean.cardano.yaci.store.api.blocks.BlocksApiConfiguration;
import com.bloxbean.cardano.yaci.store.blocks.BlocksStoreConfiguration;
import com.bloxbean.cardano.yaci.store.blocks.BlocksStoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(BlocksStoreAutoConfigProperties.class)
@Import({BlocksStoreConfiguration.class, BlocksApiConfiguration.class})
@Slf4j
public class BlocksStoreAutoConfiguration {

    @Autowired
    BlocksStoreAutoConfigProperties properties;

    @Bean
    public BlocksStoreProperties blocksStoreProperties() {
        var blocksStoreProperties = new BlocksStoreProperties();

        blocksStoreProperties.setEnabled(properties.getBlocks().isEnabled());
        blocksStoreProperties.setMetricsEnabled(properties.getBlocks().getMetrics().isEnabled());
        blocksStoreProperties.setMetricsUpdateInterval(properties.getBlocks().getMetrics().getUpdateInterval());
        blocksStoreProperties.setSaveCbor(properties.getBlocks().isSaveCbor());
        blocksStoreProperties.setCborPruningEnabled(properties.getBlocks().isCborPruningEnabled());
        blocksStoreProperties.setCborPruningSafeSlots(properties.getBlocks().getCborPruningSafeSlots());

        return blocksStoreProperties;
    }
}
