package com.bloxbean.cardano.yaci.store.starter.protocolparams;

import com.bloxbean.cardano.yaci.store.protocolparams.ProtocolParamsStoreConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(ProtocolParamsStoreProperties.class)
@Import(ProtocolParamsStoreConfiguration.class)
@Slf4j
public class ProtocolParamsStoreAutoConfiguration {

    @Autowired
    ProtocolParamsStoreProperties properties;
}
