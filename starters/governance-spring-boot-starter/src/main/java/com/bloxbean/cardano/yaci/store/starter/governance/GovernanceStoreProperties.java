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
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint govActionProposal = new Endpoint();
        private Endpoint votingProcedure = new Endpoint();
        private Endpoint constitution = new Endpoint();
        private Endpoint committee = new Endpoint();
        private Endpoint drep = new Endpoint();
        private Endpoint delegationVote = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }

}
