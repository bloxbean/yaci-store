package com.bloxbean.cardano.yaci.store.submit.config;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.backend.api.DefaultTransactionProcessor;
import com.bloxbean.cardano.client.backend.ogmios.http.OgmiosBackendService;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.yaci.store.submit.quicktx.supplier.YaciStoreProtocolParamsSupplier;
import com.bloxbean.cardano.yaci.store.submit.quicktx.supplier.YaciStoreUtxoSupplier;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Configuration for QuickTx builder support backed entirely by Yaci Store data.
 */
@Configuration
@Slf4j
public class QuickTxConfiguration {

    public static final String QUICKTX_UTXO_SUPPLIER_BEAN = "submitQuickTxUtxoSupplier";
    public static final String QUICKTX_PROTOCOL_SUPPLIER_BEAN = "submitQuickTxProtocolParamsSupplier";
    public static final String QUICKTX_TX_PROCESSOR_BEAN = "submitQuickTxTransactionProcessor";

    @Bean(name = QUICKTX_UTXO_SUPPLIER_BEAN)
    @ConditionalOnMissingBean(name = QUICKTX_UTXO_SUPPLIER_BEAN)
    public UtxoSupplier submitQuickTxUtxoSupplier(UtxoStorageReader reader) {
        log.info("QuickTx UtxoSupplier enabled using Yaci Store UTXO data");
        return new YaciStoreUtxoSupplier(reader);
    }

    @Bean(name = QUICKTX_PROTOCOL_SUPPLIER_BEAN)
    @ConditionalOnMissingBean(name = QUICKTX_PROTOCOL_SUPPLIER_BEAN)
    public ProtocolParamsSupplier submitQuickTxProtocolParamsSupplier(EpochParamStorage storage,
                                                                      ObjectMapper objectMapper) {
        log.info("QuickTx ProtocolParamsSupplier enabled using Yaci Store epoch parameters");
        return new YaciStoreProtocolParamsSupplier(storage, objectMapper);
    }

    @Bean(name = QUICKTX_TX_PROCESSOR_BEAN)
    @ConditionalOnProperty(name = "store.cardano.ogmios-url")
    @ConditionalOnMissingBean(name = QUICKTX_TX_PROCESSOR_BEAN)
    public TransactionProcessor submitQuickTxTransactionProcessor(Environment env) {
        String ogmiosUrl = env.getProperty("store.cardano.ogmios-url");
        if (!StringUtils.hasText(ogmiosUrl)) {
            throw new IllegalStateException("Property 'store.cardano.ogmios-url' must be set to use the TxPlan builder.");
        }

        log.info("QuickTx TransactionProcessor configured with Ogmios endpoint {}", ogmiosUrl);
        OgmiosBackendService backendService = new OgmiosBackendService(ogmiosUrl);
        return new DefaultTransactionProcessor(backendService.getTransactionService());
    }

    @Bean
    @ConditionalOnBean(name = {
            QUICKTX_UTXO_SUPPLIER_BEAN,
            QUICKTX_PROTOCOL_SUPPLIER_BEAN,
            QUICKTX_TX_PROCESSOR_BEAN
    })
    @ConditionalOnMissingBean(QuickTxBuilder.class)
    public QuickTxBuilder quickTxBuilder(
            @Qualifier(QUICKTX_UTXO_SUPPLIER_BEAN) UtxoSupplier utxoSupplier,
            @Qualifier(QUICKTX_PROTOCOL_SUPPLIER_BEAN) ProtocolParamsSupplier protocolParamsSupplier,
            @Qualifier(QUICKTX_TX_PROCESSOR_BEAN) TransactionProcessor transactionProcessor) {
        log.info("QuickTx builder initialized with Yaci Store suppliers");
        return new QuickTxBuilder(utxoSupplier, protocolParamsSupplier, transactionProcessor);
    }
}
