package com.bloxbean.cardano.yaci.store.cip139.protocolparameters;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = {"store.extensions.cip139.protocol-parameters.enabled"},
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.cip139.protocolparameters"})
public class ProtocolParametersConfiguration {

}
