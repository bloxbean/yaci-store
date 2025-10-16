package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model representing detailed committee member information.
 * Contains committee member status, term details, and registration info.
 */
public record CommitteeMemberDetails(
    String hash,                    // Cold credential hash
    String credType,                // Credential type: KEY_HASH or SCRIPT_HASH
    Integer startEpoch,             // Epoch when term starts
    Integer expiredEpoch,           // Epoch when term expires
    Integer currentEpoch,           // Current epoch (for context)
    Long slot,                      // Slot of last update
    String hotKey,                  // Hot key credential (from registration)
    String registrationTxHash,      // Transaction that registered this member
    Long registrationSlot,          // Slot when registered
    String deregistrationTxHash,    // Transaction that deregistered (if applicable)
    Long deregistrationSlot,        // Slot when deregistered (if applicable)
    String anchorUrl,               // Anchor URL from deregistration (if applicable)
    String anchorHash,              // Anchor hash from deregistration (if applicable)
    Boolean isActive                // Whether member is currently active
) {}
