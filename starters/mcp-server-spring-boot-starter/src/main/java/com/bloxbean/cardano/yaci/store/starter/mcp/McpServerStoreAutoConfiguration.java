package com.bloxbean.cardano.yaci.store.starter.account;

import com.bloxbean.cardano.yaci.store.account.AccountStoreConfiguration;
import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.api.account.AccountApiConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(McpServerStoreAutoConfigProperties.class)
//@Import({AccountStoreConfiguration.class, AccountApiConfiguration.class})
@Slf4j
public class McpServerStoreAutoConfiguration {

    @Autowired
    McpServerStoreAutoConfigProperties properties;

    @Bean
    public McpServerStoreProperties mcpServerStoreProperties() {
        return null;
    }
}
