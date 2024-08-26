package com.bloxbean.cardano.yaci.store.starter.epoch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class EpochStoreProperties {
    private Epoch epoch;

    @Getter
    @Setter
    public static final class Epoch {
       private boolean enabled = true;
       private boolean apiEnabled = true;
       private Endpoints endpoints = new Endpoints();

        // n2c data configuration
        private boolean n2cProtocolParamEnabled = true;
        private int n2cProtocolParamFetchingIntervalInMinutes = 5;
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint epoch = new Endpoint();
        private Endpoint network = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }
}
