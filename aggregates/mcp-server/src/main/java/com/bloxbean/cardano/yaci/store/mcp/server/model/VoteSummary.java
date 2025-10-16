package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model representing aggregated voting statistics for a proposal.
 * Provides breakdown of votes by type and voter category.
 */
public record VoteSummary(
    String govActionTxHash,         // Proposal transaction hash
    Integer govActionIndex,         // Proposal index
    Integer totalVotes,             // Total number of votes cast
    Integer yesVotes,               // Number of YES votes
    Integer noVotes,                // Number of NO votes
    Integer abstainVotes,           // Number of ABSTAIN votes
    Integer drepVotes,              // Votes from DReps
    Integer spoVotes,               // Votes from SPOs
    Integer committeeVotes,         // Votes from Constitutional Committee
    Double yesPercentage,           // Percentage of YES votes
    Double noPercentage,            // Percentage of NO votes
    Double abstainPercentage        // Percentage of ABSTAIN votes
) {}
