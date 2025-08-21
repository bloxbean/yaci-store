package com.bloxbean.cardano.yaci.store.governancerules.api;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.math.BigInteger;

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
        BigInteger noVoteStake;
        
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
        BigInteger abstainVoteStake;
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
        Integer yesVote;
        Integer noVote;
        BigDecimal threshold;
        
        public boolean hasVotes() {
            return yesVote != null || noVote != null || threshold != null;
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
}
