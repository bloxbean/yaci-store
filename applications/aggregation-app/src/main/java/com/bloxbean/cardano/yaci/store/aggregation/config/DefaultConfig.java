package com.bloxbean.cardano.yaci.store.aggregation.config;

import com.bloxbean.cardano.yaci.store.aggregation.storage.DummyDBJpaUtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.JpaTxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.JpaUtxoRepository;
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
    public UtxoStorage utxoStorage(JpaUtxoRepository jpaUtxoRepository, JpaTxInputRepository spentOutputRepository,
                                   DSLContext dsl, UtxoCache utxoCache) {
        return new DummyDBJpaUtxoStorage(jpaUtxoRepository, spentOutputRepository, dsl, utxoCache);
    }
}
