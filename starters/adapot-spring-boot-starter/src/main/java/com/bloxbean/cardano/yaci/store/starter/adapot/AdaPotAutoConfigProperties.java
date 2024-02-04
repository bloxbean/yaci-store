package com.bloxbean.cardano.yaci.store.starter.adapot;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class AdaPotAutoConfigProperties {

    private AdaPot adaPot;

    @Getter
    @Setter
    public static final class AdaPot {
        private boolean enabled = false;
        private boolean apiEnabled = true;
    }

}
