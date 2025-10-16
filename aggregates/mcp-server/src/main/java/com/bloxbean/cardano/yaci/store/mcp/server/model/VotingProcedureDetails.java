package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model representing detailed voting procedure information.
 * Contains all information about a vote cast on a governance proposal.
 */
public record VotingProcedureDetails(
    String txHash,                  // Transaction hash containing the vote
    Integer idx,                    // Vote index in transaction
    Integer txIndex,                // Transaction index in block
    String voterType,               // Voter type: DREP, SPO, COMMITTEE
    String voterHash,               // Hash identifying the voter
    String govActionTxHash,         // Proposal transaction hash being voted on
    Integer govActionIndex,         // Proposal index
    String vote,                    // Vote: YES, NO, ABSTAIN
    String anchorUrl,               // Optional metadata anchor URL
    String anchorHash,              // Optional metadata anchor hash
    Integer epoch,                  // Epoch when vote was cast
    Long slot,                      // Slot when vote was cast
    Long blockTime                  // Block timestamp
) {}
