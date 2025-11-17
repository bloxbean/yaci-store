package com.bloxbean.cardano.yaci.store.submit.config;

import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
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
 * Configuration for QuickTx builder support.
 * Provides a dedicated {@link BackendService} and {@link QuickTxBuilder} beans
 * when Blockfrost credentials are configured.
 */
@Configuration
@Slf4j
public class QuickTxConfiguration {

    public static final String QUICKTX_BACKEND_BEAN = "submitQuickTxBackendService";

    @Bean(name = QUICKTX_BACKEND_BEAN)
    @ConditionalOnMissingBean(name = QUICKTX_BACKEND_BEAN)
    @ConditionalOnProperty(name = "store.cardano.blockfrost.project-id")
    public BackendService submitQuickTxBackendService(Environment env) {
        String blockfrostUrl = env.getProperty("store.cardano.blockfrost.api-url");
        String blockfrostProjectId = env.getProperty("store.cardano.blockfrost.project-id");

        if (!StringUtils.hasText(blockfrostUrl) || !StringUtils.hasText(blockfrostProjectId)) {
            throw new IllegalStateException("Both 'store.cardano.blockfrost.api-url' and 'store.cardano.blockfrost.project-id' must be set to use TxPlan builder.");
        }

        log.info("Initializing QuickTx BackendService with Blockfrost URL: {}", blockfrostUrl);
        return new BFBackendService(blockfrostUrl, blockfrostProjectId);
    }

    @Bean
    @ConditionalOnBean(name = QUICKTX_BACKEND_BEAN)
    @ConditionalOnMissingBean
    public QuickTxBuilder quickTxBuilder(@Qualifier(QUICKTX_BACKEND_BEAN) BackendService backendService) {
        return new QuickTxBuilder(backendService);
    }
}

