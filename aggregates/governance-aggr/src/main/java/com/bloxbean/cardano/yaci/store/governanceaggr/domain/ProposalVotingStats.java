package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    private BigInteger spoYesVoteStake;
    private BigInteger spoNoVoteStake;
    private BigInteger spoAbstainVoteStake;
    private BigInteger spoDoNotVoteStake;
    private BigDecimal spoApprovalRatio;

    // dReps stats
    private BigInteger drepTotalYesStake;
    private BigInteger drepTotalNoStake;
    private BigInteger drepTotalAbstainStake;
    private BigInteger drepYesVoteStake;
    private BigInteger drepNoVoteStake;
    private BigInteger drepAbstainVoteStake;
    private BigInteger drepNoConfidenceStake;
    private BigInteger drepAutoAbstainStake;
    private BigInteger drepDoNotVoteStake;
    private BigDecimal drepApprovalRatio;

    // committee stats
    private Integer ccYes;
    private Integer ccNo;
    private Integer ccDoNotVote;
    private Integer ccAbstain;
    private BigDecimal ccApprovalRatio;
}
