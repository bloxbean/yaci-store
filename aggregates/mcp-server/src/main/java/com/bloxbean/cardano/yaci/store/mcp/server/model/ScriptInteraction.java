package com.bloxbean.cardano.yaci.store.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.lovelaceToAda;

/**
 * Information about an address interacting with a smart contract.
 * Tracks interaction frequency and value locked by each address.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScriptInteraction(
    String address,
    Long interactionCount,       // Number of times this address interacted with the script
    BigInteger valueLockedLovelace,  // Total value locked by this address (in lovelace)
    BigDecimal valueLockedAda,        // Total value locked by this address (in ADA)
    Long firstInteractionSlot,   // First slot where address interacted
    Long lastInteractionSlot     // Most recent interaction slot
) {
    /**
     * Factory method to create ScriptInteraction with automatic lovelace to ADA conversion.
     */
    public static ScriptInteraction create(
            String address,
            Long interactionCount,
            BigInteger valueLockedLovelace,
            Long firstInteractionSlot,
            Long lastInteractionSlot) {

        return new ScriptInteraction(
                address,
                interactionCount,
                valueLockedLovelace,
                lovelaceToAda(valueLockedLovelace),
                firstInteractionSlot,
                lastInteractionSlot
        );
    }
}
