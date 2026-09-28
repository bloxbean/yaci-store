package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model representing the time information for a Cardano slot.
 * Contains both Unix timestamp formats and human-readable UTC time.
 *
 * IMPORTANT: timestampSeconds is the format used in Yaci Store (block_time).
 * Multiply by 1000 to get timestampMillis for standard date/time libraries.
 */
public record SlotTimeInfo(
    Long slot,                  // The slot number
    Long timestampSeconds,      // Unix timestamp in SECONDS (Yaci Store format)
    Long timestampMillis,       // Unix timestamp in MILLISECONDS (standard format)
    String utcTime,            // ISO 8601 formatted UTC time (e.g., "2025-10-29T10:30:45Z")
    String era                 // Era for this slot (BYRON or SHELLEY)
) {}
