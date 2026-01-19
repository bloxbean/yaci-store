package com.bloxbean.cardano.yaci.store.submit.config;

import com.bloxbean.cardano.yaci.store.submit.quicktx.signing.remote.HttpRemoteSignerClient;
import com.bloxbean.cardano.yaci.store.submit.quicktx.signing.remote.RemoteSignerClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a default HTTP RemoteSignerClient when signer registry is enabled.
 */
@Configuration
@ConditionalOnProperty(prefix = "store.submit.signer-registry", name = "enabled", havingValue = "true")
public class RemoteSignerClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(RemoteSignerClient.class)
    public RemoteSignerClient httpRemoteSignerClient(RestTemplateBuilder builder) {
        return new HttpRemoteSignerClient(builder);
    }
}
