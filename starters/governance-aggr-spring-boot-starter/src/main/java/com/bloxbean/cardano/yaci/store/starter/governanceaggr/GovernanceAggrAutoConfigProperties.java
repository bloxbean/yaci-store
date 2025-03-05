package com.bloxbean.cardano.yaci.store.starter.governanceaggr;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class GovernanceAggrAutoConfigProperties {
    private GovernanceAggr governanceAggr;

    @Getter
    @Setter
    public static final class GovernanceAggr {
        private boolean enabled = false;
        private boolean apiEnabled = true;
        private Endpoints endpoints = new Endpoints();
        private boolean conwayBootstrapAvailable = false;
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint governance = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }
}
