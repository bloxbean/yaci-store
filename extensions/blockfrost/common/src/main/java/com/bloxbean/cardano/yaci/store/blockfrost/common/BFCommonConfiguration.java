package com.bloxbean.cardano.yaci.store.blockfrost.common;

import com.bloxbean.cardano.yaci.store.blockfrost.common.timeout.BFQueryTimeoutExecuteListener;
import com.bloxbean.cardano.yaci.store.blockfrost.common.timeout.BFQueryTimeoutInterceptor;
import com.bloxbean.cardano.yaci.store.blockfrost.common.timeout.BFQueryTimeoutProperties;
import com.bloxbean.cardano.yaci.store.blockfrost.common.timeout.BFRepositoryTimeoutInterceptor;
import com.bloxbean.cardano.yaci.store.blockfrost.common.timeout.BFRepositoryTimeoutPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.ExecuteListenerProvider;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Shared Blockfrost beans: the error advices and the per-request query time budget (#1207).
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(BFQueryTimeoutProperties.class)
@ComponentScan(basePackages = "com.bloxbean.cardano.yaci.store.blockfrost.common")
public class BFCommonConfiguration {

    @Bean
    public ExecuteListenerProvider bfQueryTimeoutExecuteListenerProvider() {
        return new DefaultExecuteListenerProvider(new BFQueryTimeoutExecuteListener());
    }

    @Bean
    public static BFRepositoryTimeoutPostProcessor bfRepositoryTimeoutPostProcessor(
            ObjectProvider<PlatformTransactionManager> transactionManager) {
        return new BFRepositoryTimeoutPostProcessor(new BFRepositoryTimeoutInterceptor(transactionManager));
    }

    @Bean
    public WebMvcConfigurer bfQueryTimeoutWebMvcConfigurer(BFQueryTimeoutProperties properties) {
        log.info("Blockfrost query timeout per request: {}", properties.getQueryTimeout());
        BFQueryTimeoutInterceptor interceptor = new BFQueryTimeoutInterceptor(properties.getQueryTimeout());
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }
}
