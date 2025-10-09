package com.bloxbean.cardano.yaci.store.governancerules.voting;

import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;

public interface VotingEvaluator<T extends VotingData> {
    VotingStatus evaluate(T votingData, VotingEvaluationContext context);
}
