package com.bloxbean.cardano.yaci.store.api.mir;

import com.bloxbean.cardano.yaci.store.mir.MIRStoreProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "store.mir",
        name = "api-enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.mir"})
@EnableConfigurationProperties(MIRStoreProperties.class)
public class MIRApiConfiguration {


}
