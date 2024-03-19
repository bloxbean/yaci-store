package com.bloxbean.cardano.yaci.store.aggregation.config;

import com.bloxbean.cardano.yaci.store.aggregation.storage.DummyDBUtxoStorageImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.TxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "store.extensions.utxo-storage-type",
        havingValue = "dummydb",
        matchIfMissing = true
)
public class DefaultConfig {

    @Bean
    public UtxoStorage utxoStorage(UtxoRepository utxoRepository, TxInputRepository spentOutputRepository,
                                   DSLContext dsl, UtxoCache utxoCache) {
        return new DummyDBUtxoStorageImpl(utxoRepository, spentOutputRepository, dsl, utxoCache);
    }
}
