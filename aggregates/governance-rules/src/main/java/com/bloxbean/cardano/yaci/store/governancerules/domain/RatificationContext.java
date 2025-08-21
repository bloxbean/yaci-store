package com.bloxbean.cardano.yaci.store.governancerules.domain;

import com.bloxbean.cardano.yaci.core.model.governance.actions.GovAction;
import com.bloxbean.cardano.yaci.store.governancerules.api.GovernanceState;
import com.bloxbean.cardano.yaci.store.governancerules.api.ProposalContext;
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
    
    GovAction govAction;
    VotingData votingData;
    GovernanceState governanceState;
    ProposalContext proposalContext;

    public boolean isProposalExpired() {
        if (proposalContext.getMaxAllowedVotingEpoch() == null) {
            return false;
        }
        return governanceState.getCurrentEpoch() - proposalContext.getMaxAllowedVotingEpoch() > 1;
    }

    public boolean isLastVotingEpoch() {
        return governanceState.getCurrentEpoch() - proposalContext.getMaxAllowedVotingEpoch() == 1;
    }
    
    public boolean isCommitteeNormal() {
        return governanceState.isCommitteeNormal();
    }
    
    public boolean isNotDelayed() {
        return governanceState.isNotDelayed();
    }
    
    public boolean isBootstrapPhase() {
        return governanceState.isBootstrapPhase();
    }
}
