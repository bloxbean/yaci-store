package com.bloxbean.cardano.yaci.store.blocks;

import com.bloxbean.cardano.yaci.store.client.utxo.DummyUtxoClient;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootTestApplication {

    @Bean
    public UtxoClient utxoClient() {
        return new DummyUtxoClient();
    }

    @Bean
    public BlocksStoreProperties blocksStoreProperties() {
        var blocksStoreProperties = new BlocksStoreProperties();
        blocksStoreProperties.setEnabled(true);
        blocksStoreProperties.setMetricsEnabled(true);
        blocksStoreProperties.setSaveCbor(true);
        return blocksStoreProperties;
    }
}
