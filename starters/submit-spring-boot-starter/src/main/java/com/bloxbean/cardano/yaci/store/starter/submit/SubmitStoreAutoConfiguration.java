package com.bloxbean.cardano.yaci.store.starter.submit;

import com.bloxbean.cardano.yaci.store.submit.SubmitStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(SubmitStoreProperties.class)
@Import(SubmitStoreConfiguration.class)
@Slf4j
public class SubmitStoreAutoConfiguration {

    @Autowired
    SubmitStoreProperties properties;
}
