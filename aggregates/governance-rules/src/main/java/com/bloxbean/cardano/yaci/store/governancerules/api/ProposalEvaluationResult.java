package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import lombok.Builder;
import lombok.Value;

/**
 * Result of evaluating a single proposal.
 */
@Value
@Builder
public class ProposalEvaluationResult {
    // The proposal that was evaluated
    Proposal proposal;
    // The ratification result
    RatificationResult status;

    /**
     * Checks if this proposal was ratified.
     * 
     * @return true if status is ACCEPT
     */
    public boolean isRatified() {
        return status == RatificationResult.ACCEPT;
    }
    
    /**
     * Checks if this proposal was rejected.
     * 
     * @return true if status is REJECT
     */
    public boolean isRejected() {
        return status == RatificationResult.REJECT;
    }
    
    /**
     * Checks if this proposal continues to next epoch.
     * 
     * @return true if status is CONTINUE
     */
    public boolean isContinuing() {
        return status == RatificationResult.CONTINUE;
    }
}
