package com.bloxbean.cardano.yaci.store.starter.utxo;

import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UtxoStoreAutoConfigurationTest {

    private static UtxoStoreProperties propertiesFrom(Map<String, String> values) {
        UtxoStoreAutoConfiguration configuration = new UtxoStoreAutoConfiguration();
        configuration.properties = new Binder(new MapConfigurationPropertySource(values))
                .bindOrCreate("store", Bindable.of(UtxoStoreAutoConfigProperties.class));
        return configuration.utxoStoreProperties();
    }

    @Test
    void theUnspentTableIsOffByDefault() {
        UtxoStoreProperties properties = propertiesFrom(Map.of());
        assertThat(properties.isUnspentTableEnabled()).isFalse();
        assertThat(properties.isUnspentTableReadEnabled()).isFalse();
    }

    @Test
    void theUnspentTableFlagsBindFromStoreUtxo() {
        UtxoStoreProperties properties = propertiesFrom(Map.of(
                "store.utxo.unspent-table-enabled", "true",
                "store.utxo.unspent-table-read-enabled", "true"));
        assertThat(properties.isUnspentTableEnabled()).isTrue();
        assertThat(properties.isUnspentTableReadEnabled()).isTrue();
    }
}
