package com.bloxbean.cardano.yaci.store.starter.staking;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class StakingStoreProperties {
    private Staking staking = new Staking();

    @Getter
    @Setter
    public static final class Staking {
       private boolean enabled = com.bloxbean.cardano.yaci.store.common.config.StoreModuleConfig.defaultEnabled("staking");
       private boolean apiEnabled = true;
       private Endpoints endpoints = new Endpoints();
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint pool = new Endpoint();
        private Endpoint account = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }
}
