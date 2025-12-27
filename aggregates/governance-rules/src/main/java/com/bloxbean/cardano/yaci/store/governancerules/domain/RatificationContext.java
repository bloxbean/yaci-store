package com.bloxbean.cardano.yaci.store.governancerules.domain;

import com.bloxbean.cardano.yaci.core.model.governance.actions.GovAction;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable context object containing all data needed for ratification evaluation.
 * This is passed to strategy implementations.
 */
@Value
@Builder
public class RatificationContext {
    GovernanceContext governanceContext;

    // Max allowed voting epoch for the proposal ( = proposal creation epoch + gov action lifetime)
    Integer maxAllowedVotingEpoch;

    GovAction govAction;
    VotingData votingData;

    public boolean isOutOfLifecycle() {
        return governanceContext.getCurrentEpoch() - maxAllowedVotingEpoch > 1;
    }

    public boolean isLastRatificationOpportunity() {
        return governanceContext.getCurrentEpoch() - maxAllowedVotingEpoch == 1;
    }
    
    public boolean isCommitteeNormal() {
        return governanceContext.isCommitteeNormal();
    }
    
    public boolean isNotDelayed() {
        return governanceContext.isNotDelayed();
    }
    
    public boolean isBootstrapPhase() {
        return governanceContext.isInBootstrapPhase();
    }
}
