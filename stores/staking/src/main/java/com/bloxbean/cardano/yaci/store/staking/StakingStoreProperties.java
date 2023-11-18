package com.bloxbean.cardano.yaci.store.staking;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class StakingStoreProperties {
    private Staking staking;

    @Getter
    @Setter
    public static final class Staking  {
       private boolean enabled = true;
       private boolean apiEnabled = true;
    }

}
