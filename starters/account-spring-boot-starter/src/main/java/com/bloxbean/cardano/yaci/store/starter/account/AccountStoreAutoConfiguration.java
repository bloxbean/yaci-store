package com.bloxbean.cardano.yaci.store.starter.account;

import com.bloxbean.cardano.yaci.store.account.AccountStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(AccountStoreProperties.class)
@Import(AccountStoreConfiguration.class)
@Slf4j
public class AccountStoreAutoConfiguration {

    @Autowired
    AccountStoreProperties properties;
}
