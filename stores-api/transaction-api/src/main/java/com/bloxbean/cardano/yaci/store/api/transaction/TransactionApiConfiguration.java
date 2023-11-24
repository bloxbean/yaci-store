package com.bloxbean.cardano.yaci.store.api.transaction;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = {"store.transaction.enabled", "store.transaction.api-enabled"},
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.transaction"})
public class TransactionApiConfiguration {

}
