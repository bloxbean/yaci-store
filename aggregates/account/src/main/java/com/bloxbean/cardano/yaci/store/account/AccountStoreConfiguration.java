package com.bloxbean.cardano.yaci.store.account;

import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.account.storage.AddressTxAmountStorage;
import com.bloxbean.cardano.yaci.store.account.storage.impl.AccountBalanceStorageImpl;
import com.bloxbean.cardano.yaci.store.account.storage.impl.AddressTxAmountStorageImpl;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.AddressBalanceRepository;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.AddressTxAmountRepository;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.StakeBalanceRepository;
import com.bloxbean.cardano.yaci.store.api.account.service.AccountService;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.account",
        name = "enabled",
        havingValue = "true"
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.account"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.account"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.account"})
@EnableTransactionManagement
@EnableScheduling
public class AccountStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccountBalanceStorage accountBalanceStorage(AddressBalanceRepository addressBalanceRepository,
                                                       StakeBalanceRepository stakeBalanceRepository, DSLContext dslContext,
                                                       StoreProperties storeProperties, AccountStoreProperties accountStoreProperties) {
        return new AccountBalanceStorageImpl(addressBalanceRepository, stakeBalanceRepository, dslContext, storeProperties, accountStoreProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccountService accountService(EraStorage eraStorage) {
        return new AccountService(null, eraStorage);
    }

    @Bean
    @ConditionalOnMissingBean
    public AddressTxAmountStorage addressTxAmountStorage(AddressTxAmountRepository addressTxAmountRepository,
                                                         DSLContext dslContext, StoreProperties storeProperties) {
        return new AddressTxAmountStorageImpl(addressTxAmountRepository, dslContext, storeProperties);
    }

}
