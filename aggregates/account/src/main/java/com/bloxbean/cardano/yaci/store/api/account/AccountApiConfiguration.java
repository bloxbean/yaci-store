package com.bloxbean.cardano.yaci.store.api.account;

import com.bloxbean.cardano.yaci.store.api.account.service.AccountService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "store.account",
        name = "api-enabled",
        havingValue = "true"
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.account"})
public class AccountApiConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccountService accountService() {
        return new AccountService(null);
    }

}
