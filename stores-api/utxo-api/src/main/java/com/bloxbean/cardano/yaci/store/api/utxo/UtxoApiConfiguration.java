package com.bloxbean.cardano.yaci.store.api.utxo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@ConditionalOnProperty(name = {"store.utxo.enabled", "store.utxo.api-enabled"},
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.utxo"})
@EnableRetry
public class UtxoApiConfiguration {

}
