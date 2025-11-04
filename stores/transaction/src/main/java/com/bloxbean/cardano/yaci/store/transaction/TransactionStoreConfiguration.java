package com.bloxbean.cardano.yaci.store.transaction;

import com.bloxbean.cardano.yaci.store.transaction.storage.*;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.*;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.InvalidTransactionRepository;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnCborRepository;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnEntityRepository;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.TxnWitnessRepository;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.WithdrawalRepository;
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
    public final static String STORE_TRANSACTION_ENABLED = "store.transaction.enabled";
    public final static String STORE_TRANSACTION_PRUNING_ENABLED = "store.transaction.pruning-enabled";

    @Autowired
    private DSLContext dslContext;

    @Bean
    @ConditionalOnMissingBean
    public TransactionCborStorage transactionCborStorage(TxnCborRepository txnCborRepository) {
        return new TransactionCborStorageImpl(txnCborRepository, dslContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public TransactionStorage transactionStorage(TxnEntityRepository txnEntityRepository,
                                                 TransactionCborStorage transactionCborStorage,
                                                 TxnMapper txnMapper,
                                                 TransactionStoreProperties transactionStoreProperties) {
        return new TransactionStorageImpl(txnEntityRepository, transactionCborStorage, txnMapper, dslContext, transactionStoreProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public TransactionWitnessStorage transactionWitnessStorage(TxnWitnessRepository txnWitnessRepository, TxnMapper txnMapper) {
        return new TransactionWitnessStorageImpl(txnWitnessRepository, txnMapper, dslContext);
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

    @Bean
    @ConditionalOnMissingBean
    public WithdrawalStorage withdrawalStorage(WithdrawalRepository withdrawalRepository, TxnMapper mapper) {
        return new WithdrawalStorageImpl(withdrawalRepository, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public WithdrawalStorageReader withdrawalStorageReader(WithdrawalRepository withdrawalRepository, TxnMapper mapper) {
        return new WithdrawalStorageReaderImpl(withdrawalRepository, mapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public InvalidTransactionStorage invalidTransactionStorage(InvalidTransactionRepository invalidTransactionRepository,
                                                               TxnMapper txnMapper) {
        return new InvalidTransactionStorageImpl(invalidTransactionRepository, txnMapper);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public TransactionCborStorageReader transactionCborStorageReader(TxnCborRepository txnCborRepository) {
        return new TransactionCborStorageReaderImpl(txnCborRepository);
    }
}
