package com.bloxbean.cardano.yaci.store.mcp.server.model;

import com.bloxbean.cardano.client.transaction.spec.governance.DRepType;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.DRepDetailsDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.DRepStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * MCP-optimized version of DRepDetailsDto that includes both lovelace and ADA values.
 * This model is specifically designed for LLM consumption to prevent misinterpretation of
 * lovelace values as ADA.
 *
 * Key differences from DRepDetailsDto:
 * - Deposit is provided in BOTH lovelace and ADA
 * - VotingPower is provided in BOTH lovelace and ADA
 * - All monetary values are clearly labeled to prevent LLM confusion
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DRepDetailsMcp(
    String drepId,
    String drepHash,
    DRepType dRepType,

    // Deposit in both units
    BigInteger depositLovelace,
    BigDecimal depositAda,

    DRepStatus status,

    // Voting power in both units
    BigInteger votingPowerLovelace,
    BigDecimal votingPowerAda,

    Long registrationSlot
) {
    private static final BigDecimal LOVELACE_TO_ADA = new BigDecimal("1000000");

    /**
     * Converts lovelace to ADA with 6 decimal places precision.
     * Returns null if input is null.
     */
    private static BigDecimal lovelaceToAda(BigInteger lovelace) {
        if (lovelace == null) {
            return null;
        }
        return new BigDecimal(lovelace)
                .divide(LOVELACE_TO_ADA, 6, RoundingMode.HALF_UP);
    }

    /**
     * Factory method to create DRepDetailsMcp from core DRepDetailsDto.
     * Automatically converts all lovelace values to ADA.
     */
    public static DRepDetailsMcp from(DRepDetailsDto dto) {
        if (dto == null) {
            return null;
        }

        return new DRepDetailsMcp(
            dto.getDrepId(),
            dto.getDrepHash(),
            dto.getDRepType(),

            // Deposit in both units
            dto.getDeposit(),
            lovelaceToAda(dto.getDeposit()),

            dto.getStatus(),

            // Voting power in both units
            dto.getVotingPower(),
            lovelaceToAda(dto.getVotingPower()),

            dto.getRegistrationSlot()
        );
    }
}
