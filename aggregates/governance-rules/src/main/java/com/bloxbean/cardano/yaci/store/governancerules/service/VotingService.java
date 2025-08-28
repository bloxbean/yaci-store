package com.bloxbean.cardano.yaci.store.governancerules.service;

import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingResult;
import com.bloxbean.cardano.yaci.store.governancerules.voting.committee.CommitteeVotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.drep.DRepVotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.spo.SPOVotingEvaluator;

public class VotingService {
    
    private final CommitteeVotingEvaluator committeeEvaluator = new CommitteeVotingEvaluator();
    private final DRepVotingEvaluator drepEvaluator = new DRepVotingEvaluator();
    private final SPOVotingEvaluator spoEvaluator = new SPOVotingEvaluator();
    
    public VotingResult evaluateCommitteeVoting(VotingData votingData, VotingEvaluationContext context) {
        return committeeEvaluator.evaluate(votingData, context);
    }
    
    public VotingResult evaluateDRepVoting(VotingData votingData, VotingEvaluationContext context) {
        return drepEvaluator.evaluate(votingData, context);
    }
    
    public VotingResult evaluateSPOVoting(VotingData votingData, VotingEvaluationContext context) {
        return spoEvaluator.evaluate(votingData, context);
    }
}