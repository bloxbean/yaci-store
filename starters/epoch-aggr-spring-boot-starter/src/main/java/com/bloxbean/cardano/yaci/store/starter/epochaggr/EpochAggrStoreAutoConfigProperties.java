package com.bloxbean.cardano.yaci.store.starter.epochaggr;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class EpochAggrStoreAutoConfigProperties {

    private EpochAggr epochAggr;

    @Getter
    @Setter
    public static final class EpochAggr {
        private boolean enabled = false;
        private boolean apiEnabled = true;

        private int epochCalculationInterval = 120; //in seconds
        private boolean epochCalculationEnabled;

        private Endpoints endpoints = new Endpoints();
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint epoch = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }

}
