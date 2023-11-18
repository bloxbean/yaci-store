package com.bloxbean.cardano.yaci.store.transaction;

import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionWitnessStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionWitnessStorageReader;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.TransactionStorageImpl;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.TransactionStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.TransactionWitnessStorageImpl;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.TransactionWitnessStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnEntityRepository;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnWitnessRepository;
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

    @Bean
    @ConditionalOnMissingBean
    public TransactionWitnessStorage transactionWitnessStorage(TxnWitnessRepository txnWitnessRepository, TxnMapper txnMapper) {
        return new TransactionWitnessStorageImpl(txnWitnessRepository, txnMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public TransactionStorageReader transactionStorageReader(TxnEntityRepository txnEntityRepository, TxnMapper txnMapper) {
        return new TransactionStorageReaderImpl(txnEntityRepository, txnMapper, dslContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public TransactionWitnessStorageReader transactionWitnessStorageReader(TxnWitnessRepository txnWitnessRepository, TxnMapper txnMapper) {
        return new TransactionWitnessStorageReaderImpl(txnWitnessRepository, txnMapper);
    }
}
