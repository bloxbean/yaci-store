package com.bloxbean.cardano.yaci.store.starter.epoch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class EpochStoreProperties {
    private ProtocolParams protocolParams;

    @Getter
    @Setter
    public static final class ProtocolParams {
       private boolean enabled = true;
       private boolean apiEnabled = true;
    }

}
