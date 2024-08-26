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
        private int n2cDrepStakeFetchingIntervalInMinutes = 5;
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint govActionProposal = new Endpoint();
        private Endpoint votingProcedure = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }
}
