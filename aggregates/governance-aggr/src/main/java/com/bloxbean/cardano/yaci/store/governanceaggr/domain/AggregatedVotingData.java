package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;
import java.util.Map;

@Builder
public record AggregatedVotingData(DRepVotes drepVotes, SPOVotes spoVotes, CommitteeVotes committeeVotes) {
    @Value
    @Builder
    public static class DRepVotes {
        BigInteger yesVoteStake;
        BigInteger noVoteStake;
        BigInteger doNotVoteStake;
        BigInteger noConfidenceStake;
        BigInteger abstainVoteStake;
        BigInteger autoAbstainStake;
    }

    @Value
    @Builder
    public static class SPOVotes {
        BigInteger yesVoteStake;
        BigInteger abstainVoteStake;
        BigInteger noVoteStake;
        BigInteger doNotVoteStake;
        BigInteger delegateToAutoAbstainDRepStake;
        BigInteger delegateToNoConfidenceDRepStake;
        BigInteger totalStake;
    }

    @Value
    @Builder
    public static class CommitteeVotes {
        Map<String, Vote> votes;
    }
}

