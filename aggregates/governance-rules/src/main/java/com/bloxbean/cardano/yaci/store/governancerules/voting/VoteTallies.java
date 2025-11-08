package com.bloxbean.cardano.yaci.store.governancerules.voting;

import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;

@Value
@Builder
public class VoteTallies {
    DRepTallies drep;
    SPOTallies spo;
    CommitteeTallies committee;

    @Value
    @Builder
    public static class DRepTallies {
        BigInteger totalYesStake;
        BigInteger totalNoStake;
    }

    @Value
    @Builder
    public static class SPOTallies {
        BigInteger totalYesStake;
        BigInteger totalNoStake;
        BigInteger totalAbstainStake;
    }

    @Value
    @Builder
    public static class CommitteeTallies {
        int yesCount;
        int noCount;
        int abstainCount;
        int doNotVoteCount;
    }
}

