package com.bloxbean.cardano.yaci.store.transaction;

import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.transaction.storage.api.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.TransactionStorageImpl;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.repository.TxnEntityRepository;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Configuration
@ConditionalOnProperty(
        prefix = "store.transaction",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.transaction"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.transaction"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.transaction"})
@EnableTransactionManagement
public class TransactionStoreConfiguration {

    @Autowired
    private DSLContext dslContext;

    @Bean
    @ConditionalOnMissingBean
    public TransactionStorage transactionStorage(TxnEntityRepository txnEntityRepository, TxnMapper txnMapper) {
        return new TransactionStorageImpl(txnEntityRepository, txnMapper, dslContext);
    }

    /**
     * This is a dummy utxo client. This will be used when no UtxoClient implementation is provided.
     * @return UtxoClient
     */
    @Bean
    @ConditionalOnMissingBean
    public UtxoClient utxoClient() {
        return new UtxoClient() {
            @Override
            public List<AddressUtxo> getUtxosByIds(List<UtxoKey> utxoIds) {
                return Collections.emptyList();
            }

            @Override
            public Optional<AddressUtxo> getUtxoById(UtxoKey utxoId) {
                return Optional.empty();
            }
        };
    }
}
