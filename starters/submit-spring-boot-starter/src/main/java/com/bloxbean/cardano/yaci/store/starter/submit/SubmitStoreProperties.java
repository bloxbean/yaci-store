package com.bloxbean.cardano.yaci.store.starter.submit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class SubmitStoreProperties {
    private Submit submit;

    @Getter
    @Setter
    public static final class Submit  {
       private boolean enabled = true;
    }

}
