package com.bloxbean.cardano.yaci.store.utxo;

import com.bloxbean.cardano.yaci.store.utxo.storage.api.InvalidTransactionStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.InvalidTransactionStorageImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.UtxoStorageImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.InvalidTransactionRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.UtxoRepository;
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
    public UtxoStorage utxoStorage(UtxoRepository utxoRepository, DSLContext dslContext) {
        return new UtxoStorageImpl(utxoRepository, dslContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public InvalidTransactionStorage invalidTransactionStorage(InvalidTransactionRepository invalidTransactionRepository) {
        return new InvalidTransactionStorageImpl(invalidTransactionRepository);
    }
}
