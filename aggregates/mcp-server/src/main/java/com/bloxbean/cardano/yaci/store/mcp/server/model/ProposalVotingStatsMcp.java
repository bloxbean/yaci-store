package com.bloxbean.cardano.yaci.store.mcp.server.model;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.ProposalVotingStats;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.lovelaceToAda;

/**
 * MCP-optimized version of ProposalVotingStats that includes both lovelace and ADA values.
 * This model is specifically designed for LLM consumption to prevent misinterpretation of
 * lovelace values as ADA (which would be off by a factor of 1,000,000).
 *
 * All stake/voting power fields are provided in BOTH units:
 * - *Lovelace fields: Raw values from the blockchain (1 ADA = 1,000,000 lovelace)
 * - *Ada fields: Human-readable values converted by dividing lovelace by 1,000,000
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProposalVotingStatsMcp(
    // SPO (Stake Pool Operator) voting stats
    BigInteger spoYesStakeLovelace,
    BigDecimal spoYesStakeAda,

    BigInteger spoNoStakeLovelace,
    BigDecimal spoNoStakeAda,

    BigInteger spoAbstainStakeLovelace,
    BigDecimal spoAbstainStakeAda,

    // DRep (Delegated Representative) voting stats
    BigInteger drepYesStakeLovelace,
    BigDecimal drepYesStakeAda,

    BigInteger drepNoStakeLovelace,
    BigDecimal drepNoStakeAda,

    BigInteger drepNoVoteStakeLovelace,
    BigDecimal drepNoVoteStakeAda,

    BigInteger drepNotVotedStakeLovelace,
    BigDecimal drepNotVotedStakeAda,

    BigInteger drepNoConfidenceStakeLovelace,
    BigDecimal drepNoConfidenceStakeAda,

    BigInteger drepAbstainStakeLovelace,
    BigDecimal drepAbstainStakeAda,

    // Constitutional Committee voting counts (already in correct units)
    Integer committeeYesVotes,
    Integer committeeNoVotes,
    Integer committeeDoNotVoteCount,
    Integer committeeAbstainVotes
) {
    private static final BigDecimal LOVELACE_TO_ADA = new BigDecimal("1000000");

    /**
     * Factory method to create ProposalVotingStatsMcp from core ProposalVotingStats.
     * Automatically converts all lovelace values to ADA.
     */
    public static ProposalVotingStatsMcp from(ProposalVotingStats stats) {
        if (stats == null) {
            return null;
        }

        return new ProposalVotingStatsMcp(
            // SPO stats
            stats.getSpoTotalYesStake(),
            lovelaceToAda(stats.getSpoTotalYesStake()),

            stats.getSpoTotalNoStake(),
            lovelaceToAda(stats.getSpoTotalNoStake()),

            stats.getSpoTotalAbstainStake(),
            lovelaceToAda(stats.getSpoTotalAbstainStake()),

            // DRep stats
            stats.getDrepTotalYesStake(),
            lovelaceToAda(stats.getDrepTotalYesStake()),

            stats.getDrepTotalNoStake(),
            lovelaceToAda(stats.getDrepTotalNoStake()),

            stats.getDrepNoVoteStake(),
            lovelaceToAda(stats.getDrepNoVoteStake()),

            stats.getDrepNotVotedStake(),
            lovelaceToAda(stats.getDrepNotVotedStake()),

            stats.getDrepNoConfidenceStake(),
            lovelaceToAda(stats.getDrepNoConfidenceStake()),

            stats.getDrepTotalAbstainStake(),
            lovelaceToAda(stats.getDrepTotalAbstainStake()),

            // Committee stats (no conversion needed)
            stats.getCcYes(),
            stats.getCcNo(),
            stats.getCcDoNotVote(),
            stats.getCcAbstain()
        );
    }
}
