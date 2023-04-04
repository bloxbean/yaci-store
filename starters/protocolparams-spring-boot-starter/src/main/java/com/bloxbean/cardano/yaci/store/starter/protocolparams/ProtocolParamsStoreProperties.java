package com.bloxbean.cardano.yaci.store.starter.protocolparams;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class ProtocolParamsStoreProperties {
    private ProtocolParams protocolParams;

    @Getter
    @Setter
    public static final class ProtocolParams {
       private boolean enabled = true;
    }

}
