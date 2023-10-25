package com.bloxbean.cardano.yaci.store.starter.remote;

import com.bloxbean.cardano.yaci.store.remote.RemoteProperties;
import com.bloxbean.cardano.yaci.store.remote.common.RemoteConfigProperties;
import com.bloxbean.cardano.yaci.store.remote.consumer.RemoteConsumerConfiguration;
import com.bloxbean.cardano.yaci.store.remote.publisher.RemotePublisherConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@EnableConfigurationProperties(RemoteStoreAutoConfProperties.class)
@Import({RemoteConfigProperties.class, RemotePublisherConfiguration.class, RemoteConsumerConfiguration.class})
@PropertySource("classpath:remote-config.properties")
@Slf4j
public class RemoteStoreAutoConfiguration {

    @Autowired
    RemoteStoreAutoConfProperties properties;

    @Bean
    public RemoteProperties remoteProperties() {
        RemoteProperties remoteProperties = new RemoteProperties();
        remoteProperties.setPublisherEnabled(properties.getRemote().isPublisherEnabled());
        remoteProperties.setConsumerEnabled(properties.getRemote().isConsumerEnabled());
        remoteProperties.setPublisherEvents(properties.getRemote().getPublisherEvents());

        return remoteProperties;
    }
}
