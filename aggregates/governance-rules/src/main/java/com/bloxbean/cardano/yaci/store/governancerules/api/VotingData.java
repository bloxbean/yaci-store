package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;
import java.util.Map;

/**
 * Encapsulates all voting-related data.
 * Different action types will use different subsets of this data.
 */
@Value
@Builder
public class VotingData {

    DRepVotes drepVotes;
    SPOVotes spoVotes;
    CommitteeVotes committeeVotes;
    
    /**
     * DRep voting information
     */
    @Value
    @Builder
    public static class DRepVotes {
        BigInteger yesVoteStake;
        BigInteger noConfidenceStake;
        BigInteger noVoteStake;
        BigInteger doNotVoteStake;
    }
    
    /**
     * SPO voting information
     */
    @Value
    @Builder
    public static class SPOVotes {
        BigInteger yesVoteStake;
        BigInteger delegateToAutoAbstainDRepStake;
        BigInteger delegateToNoConfidenceDRepStake;
        BigInteger abstainVoteStake;
        BigInteger doNotVoteStake;
        BigInteger totalStake;
    }
    
    /**
     * Constitutional Committee voting information
     */
    @Value
    @Builder
    public static class CommitteeVotes {
        // (hot key, vote)
        Map<String, Vote> votes;
    }
    

    /**
     * Validates the voting data.
     *
     * @throws IllegalArgumentException if voting data is invalid
     */
    public void validate() {
        //TODO
    }
}
