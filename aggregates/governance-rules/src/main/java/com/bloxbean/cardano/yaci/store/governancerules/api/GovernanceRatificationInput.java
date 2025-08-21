package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.core.model.governance.actions.GovAction;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable input object containing all data needed for governance ratification evaluation.
 * This class encapsulates the complexity of different parameter requirements for each action type.
 */
@Value
@Builder
public class GovernanceRatificationInput {
    
    // Core governance action
    GovAction govAction;
    
    // Voting data (optional fields based on action type)
    VotingData votingData;
    
    // Governance state
    GovernanceState governanceState;
    
    // Proposal context
    ProposalContext proposalContext;
    
    /**
     * Converts this input to a RatificationContext for evaluator processing.
     *
     * @return The ratification context
     */
    public RatificationContext toContext() {
        return RatificationContext.builder()
            .govAction(govAction)
            .votingData(votingData)
            .governanceState(governanceState)
            .proposalContext(proposalContext)
            .build();
    }

    /**
     * Validates that all required data for the specific action type is present.
     *
     * @throws IllegalArgumentException if required data is missing
     */
    public void validate() {
        if (govAction == null) {
            throw new IllegalArgumentException("Governance action is required");
        }
        if (governanceState == null) {
            throw new IllegalArgumentException("Governance state is required");
        }

        // Action-specific validation will be handled by individual evaluators
    }
}
