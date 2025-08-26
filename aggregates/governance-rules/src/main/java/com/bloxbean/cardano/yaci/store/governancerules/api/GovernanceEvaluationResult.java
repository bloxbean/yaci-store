package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Comprehensive result of governance state evaluation.
 * Contains individual proposal results and proposals to drop.
 */
@Value
@Builder
public class GovernanceEvaluationResult {

    // Individual evaluation results for each proposal
    List<ProposalEvaluationResult> proposalResults;

    // Proposals that should be dropped in the next epoch
    List<Proposal> proposalsToDropNext;

    // Whether action ratification is delayed for this epoch
    // True if any delaying action (NoConfidence, UpdateCommittee, NewConstitution, HardFork) was ratified
    boolean isActionRatificationDelayed;

}
