package com.bloxbean.cardano.yaci.store.starter.remote;

import com.bloxbean.cardano.yaci.store.remote.common.RemoteConfigProperties;
import com.bloxbean.cardano.yaci.store.remote.consumer.RemoteConsumerConfiguration;
import com.bloxbean.cardano.yaci.store.remote.publisher.RemotePublisherConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@EnableConfigurationProperties(RemoteStoreProperties.class)
@Import({RemoteConfigProperties.class, RemotePublisherConfiguration.class, RemoteConsumerConfiguration.class})
@PropertySource("classpath:remote-config.properties")
@Slf4j
public class RemoteStoreAutoConfiguration {

    @Autowired
    RemoteStoreProperties properties;
}
