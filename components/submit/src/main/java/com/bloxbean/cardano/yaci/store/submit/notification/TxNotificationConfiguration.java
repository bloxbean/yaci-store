package com.bloxbean.cardano.yaci.store.submit.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Configuration for transaction notification channels.
 */
@Configuration
@EnableAsync
@Slf4j
public class TxNotificationConfiguration {
    
    /**
     * RestTemplate for webhook notifications.
     * Only created if webhook is enabled.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "store.submit.lifecycle.webhook",
            name = "enabled",
            havingValue = "true"
    )
    public RestTemplate webhookRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(10000))
                .build();
    }
    
    /**
     * Async executor for notification channels.
     * Ensures notifications don't block the main transaction flow.
     */
    @Bean(name = "txNotificationExecutor")
    public Executor txNotificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("tx-notification-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

