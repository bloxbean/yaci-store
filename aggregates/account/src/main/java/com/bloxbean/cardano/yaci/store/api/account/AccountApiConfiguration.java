package com.bloxbean.cardano.yaci.store.api.account;

import com.bloxbean.cardano.yaci.store.api.account.service.AccountService;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    public AccountService accountService(EraStorage eraStorage) {
        return new AccountService(null, eraStorage);
    }

}
