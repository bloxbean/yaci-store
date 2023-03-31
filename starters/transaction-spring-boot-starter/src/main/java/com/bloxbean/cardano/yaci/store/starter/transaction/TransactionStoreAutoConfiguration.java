package com.bloxbean.cardano.yaci.store.starter.transaction;

import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(TransactionStoreProperties.class)
@Import(TransactionStoreConfiguration.class)
@Slf4j
public class TransactionStoreAutoConfiguration {

    @Autowired
    TransactionStoreProperties properties;
}
