package com.bloxbean.cardano.yaci.store.mir;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class MIRStoreProperties {
    private MIR mir;

    @Getter
    @Setter
    public static final class MIR  {
       private boolean enabled = true;
    }

}
