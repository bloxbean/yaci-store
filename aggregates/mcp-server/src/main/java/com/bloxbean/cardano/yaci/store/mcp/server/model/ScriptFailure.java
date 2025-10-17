package com.bloxbean.cardano.yaci.store.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Information about a failed script execution.
 * Useful for debugging smart contract issues and understanding failure patterns.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScriptFailure(
    String txHash,
    Long slot,
    String blockHash,
    String scriptHash,
    String purpose,              // spend, mint, cert, reward, vote
    String datumHash,
    String redeemerDataHash,
    Long unitMem,                // Memory units attempted
    Long unitSteps               // CPU steps attempted
) {
}
