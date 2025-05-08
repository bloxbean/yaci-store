package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

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
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProposalVotingStats {
    // SPO stats
    private BigInteger spoTotalYesStake;
    private BigInteger spoTotalNoStake;
    private BigInteger spoTotalAbstainStake;

    // dReps stats
    private BigInteger drepTotalYesStake;

    private BigInteger drepTotalNoStake;
    private BigInteger drepNoVoteStake;
    private BigInteger drepNotVotedStake;
    private BigInteger drepNoConfidenceStake;

    private BigInteger drepTotalAbstainStake;

    // committee stats
    private Integer ccYes;
    private Integer ccNo;
    private Integer ccDoNotVote;
    private Integer ccAbstain;
}
