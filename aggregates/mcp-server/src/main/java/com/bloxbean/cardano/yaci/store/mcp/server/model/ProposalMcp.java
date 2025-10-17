package com.bloxbean.cardano.yaci.store.mcp.server.model;

import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.lovelaceToAda;

/**
 * MCP-optimized version of ProposalDto that includes both lovelace and ADA values.
 * This model is specifically designed for LLM consumption to prevent misinterpretation of
 * lovelace values as ADA.
 *
 * Key differences from ProposalDto:
 * - Deposit is provided in BOTH lovelace and ADA
 * - VotingStats is replaced with ProposalVotingStatsMcp (which has dual units)
 * - All monetary values are clearly labeled to prevent LLM confusion
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ProposalMcp(
    String txHash,
    int index,
    Long slot,

    // Deposit in both units
    BigInteger depositLovelace,
    BigDecimal depositAda,

    String returnAddress,
    JsonNode govAction,
    String anchorUrl,
    String anchorHash,
    ProposalStatus status,

    // MCP-optimized voting stats with dual units
    ProposalVotingStatsMcp votingStatsMcp,

    Integer epoch,
    Long blockNumber,
    Long blockTime
) {
    private static final BigDecimal LOVELACE_TO_ADA = new BigDecimal("1000000");

    /**
     * Converts lovelace to ADA with 6 decimal places precision.
     * Returns null if input is null.
     */

    /**
     * Factory method to create ProposalMcp from core ProposalDto.
     * Automatically converts all lovelace values to ADA and wraps voting stats.
     */
    public static ProposalMcp from(ProposalDto dto) {
        if (dto == null) {
            return null;
        }

        return new ProposalMcp(
            dto.getTxHash(),
            dto.getIndex(),
            dto.getSlot(),

            // Deposit in both units
            dto.getDeposit(),
            lovelaceToAda(dto.getDeposit()),

            dto.getReturnAddress(),
            dto.getGovAction(),
            dto.getAnchorUrl(),
            dto.getAnchorHash(),
            dto.getStatus(),

            // Convert voting stats to MCP version
            ProposalVotingStatsMcp.from(dto.getVotingStats()),

            dto.getEpoch(),
            dto.getBlockNumber(),
            dto.getBlockTime()
        );
    }
}
