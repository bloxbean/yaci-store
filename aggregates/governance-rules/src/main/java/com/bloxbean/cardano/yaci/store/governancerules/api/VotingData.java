package com.bloxbean.cardano.yaci.store.governancerules.api;

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

        public boolean hasVotes() {
            return yesVoteStake != null || noVoteStake != null;
        }
    }
    
    /**
     * SPO voting information
     */
    @Value
    @Builder
    public static class SPOVotes {
        BigInteger yesVoteStake;
        BigInteger delegateToAutoAbstainDRepStake;
        BigInteger abstainVoteStake;
        BigInteger doNotVoteStake;
        BigInteger totalStake;
        
        public boolean hasVotes() {
            return yesVoteStake != null || abstainVoteStake != null || totalStake != null;
        }
    }
    
    /**
     * Constitutional Committee voting information
     */
    @Value
    @Builder
    public static class CommitteeVotes {
        // (hot key, vote)
        Map<String, Vote> votes;

        public boolean hasVotes() {
            return votes != null && !votes.isEmpty();
        }
    }
    
    public boolean hasDRepVotes() {
        return drepVotes != null && drepVotes.hasVotes();
    }
    
    public boolean hasSPOVotes() {
        return spoVotes != null && spoVotes.hasVotes();
    }
    
    public boolean hasCommitteeVotes() {
        return committeeVotes != null && committeeVotes.hasVotes();
    }

    /**
     * Validates the voting data.
     *
     * @throws IllegalArgumentException if voting data is invalid
     */
    public void validate() {
        if (drepVotes == null && spoVotes == null && committeeVotes == null) {
            throw new IllegalArgumentException("At least one type of voting data must be provided");
        }
    }
}
