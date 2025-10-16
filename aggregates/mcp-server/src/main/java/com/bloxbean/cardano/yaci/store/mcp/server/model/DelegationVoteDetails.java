package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model representing delegation vote certificate details.
 * Shows which addresses are delegating voting power to which DReps.
 */
public record DelegationVoteDetails(
    String txHash,                  // Transaction containing the delegation certificate
    Long certIndex,                 // Certificate index in transaction
    Integer txIndex,                // Transaction index in block
    String address,                 // Stake address delegating voting power
    String drepHash,                // DRep credential hash being delegated to
    String drepId,                  // DRep ID in bech32 format (drep1...)
    String drepType,                // DRep type: KEY_HASH, SCRIPT_HASH, ABSTAIN, NO_CONFIDENCE
    String credential,              // Stake credential of delegator
    String credType,                // Credential type: ADDR_KEYHASH or SCRIPTHASH
    Integer epoch,                  // Epoch when delegation was made
    Long slot,                      // Slot when delegation was made
    Long blockTime                  // Block timestamp
) {}
