package com.bloxbean.cardano.yaci.store.utxo;

import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoStorageImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.TxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.utxo",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.utxo"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.utxo"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.utxo"})
@EnableTransactionManagement
public class UtxoStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public UtxoStorage utxoStorage(UtxoRepository utxoRepository, TxInputRepository spentOutputRepository, DSLContext dslContext, UtxoCache utxoCache) {
        return new UtxoStorageImpl(utxoRepository, spentOutputRepository, dslContext, utxoCache);
    }

    @Bean
    @ConditionalOnMissingBean
    public UtxoStorageReader utxoStorageReader(UtxoRepository utxoRepository, TxInputRepository spentOutputRepository, DSLContext dslContext) {
        return new UtxoStorageReaderImpl(utxoRepository, spentOutputRepository, dslContext);
    }
}
