package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigInteger;

/**
 * Model representing a DRep's voting power at a specific epoch.
 * Tracks historical changes in DRep voting power over time.
 */
public record DRepVotingPowerHistory(
    String drepHash,                // DRep credential hash
    String drepId,                  // DRep ID in bech32 format (drep1...)
    String drepType,                // DRep type: KEY_HASH, SCRIPT_HASH, ABSTAIN, NO_CONFIDENCE
    BigInteger amount,              // Voting power (delegated stake) in lovelace
    Integer epoch,                  // Epoch for this snapshot
    Integer activeUntil,            // Epoch until which this voting power is active
    Integer expiry                  // Expiry epoch for this distribution
) {}
