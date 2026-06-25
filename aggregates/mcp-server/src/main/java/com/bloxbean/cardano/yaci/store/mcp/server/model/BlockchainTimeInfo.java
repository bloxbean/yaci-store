package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Comprehensive model containing complete time information for a blockchain event.
 * Can be constructed from either a slot number or block_time timestamp.
 * Provides all necessary time representations for displaying to users.
 *
 * IMPORTANT: blockTime is the format used in Yaci Store (Unix timestamp in SECONDS).
 */
public record BlockchainTimeInfo(
    Long slot,                  // Slot number (null if constructed from block_time only)
    Long blockTime,             // Unix timestamp in SECONDS (Yaci Store format)
    Long timestampMillis,       // Unix timestamp in MILLISECONDS (standard format)
    String utcTime,            // ISO 8601 formatted UTC time (e.g., "2025-10-29T10:30:45Z")
    String localTime,          // Formatted time in user's timezone (e.g., "October 29, 2025 at 11:08:26 AM SGT")
    String timezone,           // Timezone ID used for local time (e.g., "Asia/Singapore", "UTC")
    String era,                // Era for this slot/time (BYRON or SHELLEY)
    String conversionNote      // Help text explaining the conversion and formats
) {
    /**
     * Create BlockchainTimeInfo with a helpful conversion note.
     */
    public static BlockchainTimeInfo create(
        Long slot,
        Long blockTime,
        String utcTime,
        String localTime,
        String timezone,
        String era
    ) {
        Long timestampMillis = blockTime != null ? blockTime * 1000 : null;

        String conversionNote = String.format("""
            ⏰ TIMESTAMP CONVERSION DETAILS:
            - Block Time (Yaci Store format): %d seconds
            - Timestamp (standard format): %d milliseconds
            - UTC Time: %s
            - Local Time (%s): %s
            - Era: %s
            %s

            IMPORTANT: Yaci Store uses timestamps in SECONDS, not milliseconds.
            To use with standard date libraries, multiply by 1000.
            """,
            blockTime,
            timestampMillis,
            utcTime,
            timezone,
            localTime,
            era,
            slot != null ? "- Slot: " + slot : ""
        );

        return new BlockchainTimeInfo(
            slot,
            blockTime,
            timestampMillis,
            utcTime,
            localTime,
            timezone,
            era,
            conversionNote
        );
    }
}
