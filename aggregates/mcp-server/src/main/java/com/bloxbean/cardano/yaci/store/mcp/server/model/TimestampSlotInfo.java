package com.bloxbean.cardano.yaci.store.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Result of converting a Unix timestamp to a Cardano slot number.
 * Includes validation information, era detection, and helpful notes for LLMs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TimestampSlotInfo(
    Long originalTimestamp,      // Original input (may be seconds or millis)
    Long timestampSeconds,       // Normalized to seconds
    Long timestampMillis,        // Normalized to milliseconds
    Long slot,                   // Calculated slot number
    String era,                  // Detected era (BYRON or SHELLEY)
    String validationNote,       // Validation/rounding info
    String conversionNote        // Help text
) {
    /**
     * Factory method to create TimestampSlotInfo with validation and help text.
     */
    public static TimestampSlotInfo create(
        Long originalTimestamp,
        Long timestampSeconds,
        Long slot,
        String era,
        String validationNote
    ) {
        Long timestampMillis = timestampSeconds * 1000;

        String conversionNote = String.format("""
            ⏰ TIMESTAMP → SLOT CONVERSION:
            - Input: %d
            - Normalized: %d seconds (%d milliseconds)
            - Calculated Slot: %d
            - Era: %s
            - Validation: %s

            NOTES:
            - Slots are discrete time units (1 sec for Shelley, 20 sec for Byron)
            - Timestamps between slots are rounded to nearest slot
            - To convert back: use 'cardano-slot-to-timestamp' tool
            """,
            originalTimestamp,
            timestampSeconds,
            timestampMillis,
            slot,
            era,
            validationNote
        );

        return new TimestampSlotInfo(
            originalTimestamp,
            timestampSeconds,
            timestampMillis,
            slot,
            era,
            validationNote,
            conversionNote
        );
    }
}
