package com.bloxbean.cardano.yaci.store.governancerules.voting;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VotingResult {
    VotingStatus status;
    VoteTallies tallies;
}

