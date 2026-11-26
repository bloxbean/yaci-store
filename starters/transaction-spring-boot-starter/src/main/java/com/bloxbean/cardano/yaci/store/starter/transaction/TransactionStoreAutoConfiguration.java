package com.bloxbean.cardano.yaci.store.starter.transaction;

import com.bloxbean.cardano.yaci.core.config.YaciConfig;
import com.bloxbean.cardano.yaci.store.api.transaction.TransactionApiConfiguration;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreConfiguration;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(TransactionAutoConfigProperties.class)
@Import({TransactionStoreConfiguration.class, TransactionApiConfiguration.class})
@Slf4j
public class TransactionStoreAutoConfiguration {

    @Autowired
    TransactionAutoConfigProperties properties;

    @PostConstruct
    public void configureYaciFullTxCbor() {
        if (properties.getTransaction().isSaveFullTxCbor()) {
            YaciConfig.INSTANCE.setReturnFullTxCbor(true);
        }
    }

    @Bean
    public TransactionStoreProperties transactionStoreProperties() {
        var transactionStoreProperties = new TransactionStoreProperties();

        transactionStoreProperties.setPruningEnabled(properties.getTransaction().isPruningEnabled());
        transactionStoreProperties.setPruningInterval(properties.getTransaction().getPruningInterval());
        transactionStoreProperties.setPruningSafeSlots(properties.getTransaction().getPruningSafeSlots());
        transactionStoreProperties.setSaveWitness(properties.getTransaction().isSaveWitness());
        transactionStoreProperties.setSaveCbor(properties.getTransaction().isSaveCbor());
        transactionStoreProperties.setSaveFullTxCbor(properties.getTransaction().isSaveFullTxCbor());
        transactionStoreProperties.setCborPruningEnabled(properties.getTransaction().isCborPruningEnabled());
        transactionStoreProperties.setCborPruningSafeSlots(properties.getTransaction().getCborPruningSafeSlots());

        return transactionStoreProperties;
    }
}
