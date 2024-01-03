package com.bloxbean.cardano.yaci.store.aggregation.config;

import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.aggregation.storage.RocksDBAccountBalanceStorageImpl;
import com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb.RocksDBUtxoStorage;
import com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb.RocksDBUtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.bloxbean.rocks.types.config.RocksDBProperties;
import com.bloxbean.rocks.types.serializer.JacksonSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "store.aggr.utxo-storage-type",
        havingValue = "rocksdb",
        matchIfMissing = false
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.rocksdb"})
public class RocksDBConfig {

    @Value("${store.aggr.rocksdb.base-dir:./_rocksdb}")
    private String rocksDBBaseDir;

    @Value("${store.rocksdb.column-families:utxos,spent-utxos,account_balances,stake_account_balances}")
    private String columnFamilies;

    @Bean
    @ConditionalOnMissingBean
    public com.bloxbean.rocks.types.config.RocksDBConfig rocksDBConfig() {
        var rocksDBProperties = new RocksDBProperties();
        rocksDBProperties.setRocksDBBaseDir(rocksDBBaseDir);
        rocksDBProperties.setColumnFamilies(columnFamilies);
        var rocksDBConfig = new com.bloxbean.rocks.types.config.RocksDBConfig(rocksDBProperties);
        rocksDBConfig.setKeySerializer(new JacksonSerializer());
        rocksDBConfig.setValueSerializer(new JacksonSerializer());

        return rocksDBConfig;
    }

    @Bean
    public UtxoStorage utxoStorage(com.bloxbean.rocks.types.config.RocksDBConfig rocksDBConfig, UtxoCache utxoCache) {
        return new RocksDBUtxoStorage(rocksDBConfig, utxoCache);
    }

    @Bean
    public UtxoStorageReader utxoStorageReader(com.bloxbean.rocks.types.config.RocksDBConfig rocksDBConfig) {
        return new RocksDBUtxoStorageReader(rocksDBConfig);
    }

    @Bean
    @ConditionalOnProperty(name = "store.aggr.rocksdb-account-balance-store-enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    public AccountBalanceStorage accountBalanceStorage(com.bloxbean.rocks.types.config.RocksDBConfig rocksDBConfig) {
        return new RocksDBAccountBalanceStorageImpl(rocksDBConfig);
    }

}
