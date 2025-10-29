package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model representing a formatted timestamp with timezone information.
 * Used for displaying blockchain times in human-readable format with user's local timezone.
 *
 * IMPORTANT: timestampSeconds is the input format from Yaci Store (block_time).
 */
public record TimestampFormatted(
    Long timestampSeconds,      // Unix timestamp in SECONDS (Yaci Store format)
    Long timestampMillis,       // Unix timestamp in MILLISECONDS (standard format)
    String formattedTime,       // Formatted time in user's timezone (e.g., "October 29, 2025 at 11:08:26 AM SGT")
    String timezone,            // Timezone ID used for formatting (e.g., "Asia/Singapore", "UTC")
    String utcTime              // ISO 8601 formatted UTC time for reference
) {}
