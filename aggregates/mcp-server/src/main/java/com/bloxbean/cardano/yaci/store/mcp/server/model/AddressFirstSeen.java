package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Model representing when an address first appeared on-chain.
 * Useful for wallet age analysis and address behavior profiling.
 */
public record AddressFirstSeen(
    String address,              // Address (addr1... or addr_test...)
    Long firstSlot,              // First slot where address appeared
    Long firstBlock,             // First block number
    Long firstBlockTime,         // First block timestamp (Unix epoch seconds)
    LocalDateTime firstSeenDate, // Human-readable date/time (UTC)
    Integer firstEpoch,          // First epoch
    Long ageInDays,              // Age in days since first seen
    Long ageInEpochs             // Age in epochs since first seen
) {
    /**
     * Create AddressFirstSeen with automatic age calculations.
     */
    public static AddressFirstSeen create(
        String address,
        Long firstSlot,
        Long firstBlock,
        Long firstBlockTime,
        Integer firstEpoch
    ) {
        // Convert Unix timestamp to LocalDateTime
        LocalDateTime firstSeenDate = firstBlockTime != null
            ? LocalDateTime.ofInstant(Instant.ofEpochSecond(firstBlockTime), ZoneOffset.UTC)
            : null;

        // Calculate age in days
        Long ageInDays = firstBlockTime != null
            ? (System.currentTimeMillis() / 1000 - firstBlockTime) / (24 * 3600)
            : null;

        // Calculate age in epochs (assuming current epoch can be derived, but we'll use approximate calculation)
        // This is an approximation - for exact current epoch, would need to query epoch_param
        Long ageInEpochs = firstEpoch != null
            ? (ageInDays != null ? ageInDays / 5 : null) // Rough: 5 days per epoch
            : null;

        return new AddressFirstSeen(
            address,
            firstSlot,
            firstBlock,
            firstBlockTime,
            firstSeenDate,
            firstEpoch,
            ageInDays,
            ageInEpochs
        );
    }
}
