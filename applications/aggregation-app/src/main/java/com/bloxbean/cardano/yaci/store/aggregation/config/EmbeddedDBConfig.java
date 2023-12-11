package com.bloxbean.cardano.yaci.store.aggregation.config;

import com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb.EmbeddedUtxoStorage;
import com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb.EmbeddedUtxoStorageReader;
import com.bloxbean.cardano.yaci.store.rocksdb.config.RocksDBConfig;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
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
    public UtxoStorage utxoStorage(RocksDBConfig rocksDBConfig, UtxoCache utxoCache) {
        return new EmbeddedUtxoStorage(rocksDBConfig, utxoCache);
    }

    @Bean
    public UtxoStorageReader utxoStorageReader(RocksDBConfig rocksDBConfig) {
        return new EmbeddedUtxoStorageReader(rocksDBConfig);
    }

}
