package com.bloxbean.cardano.yaci.store.starter.governance;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class GovernanceStoreProperties {
    private Governance governance;

    @Getter
    @Setter
    public static final class Governance  {
       private boolean enabled = true;
    }

}
