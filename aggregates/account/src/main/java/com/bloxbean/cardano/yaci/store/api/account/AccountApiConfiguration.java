package com.bloxbean.cardano.yaci.store.api.account;

import com.bloxbean.cardano.yaci.store.api.account.service.AccountService;
import com.bloxbean.cardano.yaci.store.account.service.StakeAccountRewardProvider;
import com.bloxbean.cardano.yaci.store.core.service.local.LocalClientProviderManager;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import jakarta.annotation.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = {"store.account.enabled", "store.account.api-enabled"},
        havingValue = "true"
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.account"})
public class AccountApiConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccountService accountService(@Nullable LocalClientProviderManager localClientProviderManager,
                                         EraStorage eraStorage,
                                         ObjectProvider<StakeAccountRewardProvider> stakeAccountRewardProvider) {
        return new AccountService(localClientProviderManager, eraStorage, stakeAccountRewardProvider.getIfAvailable());
    }

}
