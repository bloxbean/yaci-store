package com.bloxbean.cardano.yaci.store.blockfrost.account;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = {"store.extensions.blockfrost.account.enabled"},
        havingValue = "true",
        matchIfMissing = false
)
@ComponentScan(basePackages = {
        "com.bloxbean.cardano.yaci.store.blockfrost.account",
})
public class BFAccountConfiguration {

}
