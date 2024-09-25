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
    private static final class Governance {

        private boolean enabled = true;
        private boolean apiEnabled = true;
        private Endpoints endpoints = new Endpoints();

        // n2c data configuration
        private boolean n2cGovStateEnabled = true;
        private int n2cGovStateFetchingIntervalInMinutes = 5;
        private boolean n2cDrepStakeEnabled = true;
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint proposal = new Endpoint();
        private Endpoint vote = new Endpoint();
        private EnhancedEndpoint constitution = new EnhancedEndpoint();
        private EnhancedEndpoint committee = new EnhancedEndpoint();
        private EnhancedEndpoint drep = new EnhancedEndpoint();
        private Endpoint delegationVote = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static final class EnhancedEndpoint {
        private boolean enabled = true;
        private Live live = new Live();
    }

    @Getter
    @Setter
    private static final class Live {
        private boolean enabled = true;
    }
}
