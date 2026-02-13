package com.bloxbean.cardano.yaci.store.submit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(
        prefix = "store.submit",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.submit"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.submit"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.submit"})
@EnableTransactionManagement
@EnableScheduling
public class SubmitStoreConfiguration {
    public static final String STORE_SUBMIT_ENABLED = "store.submit.enabled == 'true' || store.submit.enabled == true || store.submit.enabled == null";

    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(10000))
                .build();
    }
}
