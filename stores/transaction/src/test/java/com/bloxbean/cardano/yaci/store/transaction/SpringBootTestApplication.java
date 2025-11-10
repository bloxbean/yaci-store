package com.bloxbean.cardano.yaci.store.transaction;

import com.bloxbean.cardano.yaci.store.client.utxo.DummyUtxoClient;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.plugin.core.PluginRegistry;
import com.bloxbean.cardano.yaci.store.plugin.metrics.PluginMetricsCollector;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class SpringBootTestApplication {

    @Bean
    public UtxoClient utxoClient() {
        return new DummyUtxoClient();
    }

    @Bean
    public StoreProperties storeProperties() {
        return new StoreProperties();
    }

    @Bean
    public PluginRegistry pluginRegistry(StoreProperties storeProperties) {
        return new PluginRegistry(storeProperties, List.of(), null, new PluginMetricsCollector(storeProperties, null));
    }

    @Bean
    public TransactionStoreProperties transactionStoreProperties() {
        return new TransactionStoreProperties();
    }
}
