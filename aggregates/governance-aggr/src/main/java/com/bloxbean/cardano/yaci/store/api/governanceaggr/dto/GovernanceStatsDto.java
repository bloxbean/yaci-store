package com.bloxbean.cardano.yaci.store.api.governanceaggr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GovernanceStatsDto {
    private Integer epoch;
    private DRepStatsDto drepStats;
    private ProposalStatsDto proposalStats;
    private CommitteeStatsDto committeeStats;
    private VoteStatsDto voteStats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class DRepStatsDto {
        private int totalDreps;
        private int activeDreps;
        private int inactiveDreps;
        private int retiredDreps;
        private BigInteger totalVotingPower;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ProposalStatsDto {
        private int activeProposals;
        private int ratifiedProposals;
        private int expiredProposals;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class CommitteeStatsDto {
        private int totalMembers;
        private int activeMembers;
        private int expiredMembers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class VoteStatsDto {
        private int totalVotesCurrentEpoch;
        private int drepVotes;
        private int spoVotes;
        private int committeeVotes;
    }
}
