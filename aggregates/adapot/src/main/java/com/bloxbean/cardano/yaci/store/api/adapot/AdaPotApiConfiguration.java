package com.bloxbean.cardano.yaci.store.api.adapot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = {"store.adapot.enabled", "store.adapot.api-enabled"},
        havingValue = "true"
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.adapot"})
public class AdaPotApiConfiguration {


}
