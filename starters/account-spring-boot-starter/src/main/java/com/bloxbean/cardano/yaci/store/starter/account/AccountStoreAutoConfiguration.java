package com.bloxbean.cardano.yaci.store.starter.account;

import com.bloxbean.cardano.yaci.store.account.AccountStoreConfiguration;
import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(AccountStoreAutoConfigProperties.class)
@Import(AccountStoreConfiguration.class)
@Slf4j
public class AccountStoreAutoConfiguration {

    @Autowired
    AccountStoreAutoConfigProperties properties;

    @Bean
    public AccountStoreProperties accountStoreProperties() {
        AccountStoreProperties accountStoreProperties = new AccountStoreProperties();
        accountStoreProperties.setHistoryCleanupEnabled(properties.getAccount().isHistoryCleanupEnabled());
        accountStoreProperties.setBalanceAggregationEnabled(properties.getAccount().isBalanceAggregationEnabled());

        accountStoreProperties.setBatchBalanceAggregationEnabled(properties.getAccount().isBatchBalanceAggregationEnabled());
        accountStoreProperties.setBatchBalanceAggregationSchedulerEnabled(properties.getAccount().isBatchBalanceAggregationSchedulerEnabled());
        accountStoreProperties.setBatchBalanceAggregationScheduleDelay(properties.getAccount().getBatchBalanceAggregationScheduleDelay());
        accountStoreProperties.setBatchBalanceAggregationBatchSize(properties.getAccount().getBatchBalanceAggregationBatchSize());
        accountStoreProperties.setBatchBalanceAggregationSafeBlockDiff(properties.getAccount().getBatchBalanceAggregationSafeBlockDiff());

        return accountStoreProperties;
    }
}
