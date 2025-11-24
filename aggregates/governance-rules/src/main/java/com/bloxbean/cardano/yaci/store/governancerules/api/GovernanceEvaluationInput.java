package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommittee;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Input for governance evaluation.
 */
@Value
@Builder
public class GovernanceEvaluationInput {
    
    // Current proposals to evaluate
    List<ProposalContext> currentProposals;

    // Current governance state
    int currentEpoch;
    ProtocolParams protocolParams;
    ConstitutionCommittee committee;
    boolean isBootstrapPhase;
    BigInteger treasury;

    // Last enacted gov actions
    Map<ProposalType, GovActionId> lastEnactedGovActionIds;

    /**
     * Validates the input data.
     * 
     * @throws IllegalArgumentException if required data is missing or invalid
     */
    public void validate() {
        if (currentProposals == null || currentProposals.isEmpty()) {
            throw new IllegalArgumentException("Current proposals cannot be null or empty");
        }
        
        if (protocolParams == null) {
            throw new IllegalArgumentException("Protocol parameters are required");
        }
        
        if (committee == null) {
            throw new IllegalArgumentException("Committee information is required");
        }
        
        // Validate each proposal context
        for (ProposalContext proposal : currentProposals) {
            proposal.validate();
        }
    }

}
