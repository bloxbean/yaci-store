package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Model representing a single delegation event in staking history.
 * Tracks when a stake address changed delegation from one pool to another.
 */
public record DelegationEvent(
    String txHash,              // Transaction hash containing delegation certificate
    Integer certIndex,          // Certificate index within transaction
    String poolId,              // Pool ID (bech32 format: pool1...)
    Integer epoch,              // Epoch when delegation became active
    Long slot,                  // Slot number of delegation transaction
    Long blockTime,             // Block timestamp (Unix epoch seconds)
    LocalDateTime delegationDate // Human-readable date/time (UTC)
) {
    /**
     * Create DelegationEvent with automatic date conversion.
     */
    public static DelegationEvent create(
        String txHash,
        Integer certIndex,
        String poolId,
        Integer epoch,
        Long slot,
        Long blockTime
    ) {
        LocalDateTime delegationDate = blockTime != null
            ? LocalDateTime.ofInstant(Instant.ofEpochSecond(blockTime), ZoneOffset.UTC)
            : null;

        return new DelegationEvent(
            txHash,
            certIndex,
            poolId,
            epoch,
            slot,
            blockTime,
            delegationDate
        );
    }
}
