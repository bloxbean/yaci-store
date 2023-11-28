package com.bloxbean.cardano.yaci.store.aggregation.config;

import com.bloxbean.cardano.yaci.store.aggregation.storage.EmbeddedUtxoStorage;
import com.bloxbean.cardano.yaci.store.rocksdb.RocksDBConfig;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "store.aggr.embedded-utxo-store-enabled",
        havingValue = "true",
        matchIfMissing = false
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.rocksdb"})
public class EmbeddedDBConfig {

    @Bean
    public UtxoStorage utxoStorage(RocksDBConfig rocksDBConfig) {
        return new EmbeddedUtxoStorage(rocksDBConfig);
    }
}
