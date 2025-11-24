package com.bloxbean.cardano.yaci.store.blocks;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootTestApplication {

    @Bean
    public BlocksStoreProperties blocksStoreProperties() {
        var blocksStoreProperties = new BlocksStoreProperties();
        blocksStoreProperties.setEnabled(true);
        blocksStoreProperties.setMetricsEnabled(true);
        blocksStoreProperties.setSaveCbor(true);
        return blocksStoreProperties;
    }
}
