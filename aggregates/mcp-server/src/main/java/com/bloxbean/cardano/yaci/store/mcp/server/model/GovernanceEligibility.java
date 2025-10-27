package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model representing eligible governance participants at a specific epoch.
 * Provides baseline counts for calculating participation rates.
 *
 * Usage: Compare these eligible voter counts with actual voters from proposal-vote-summary
 * to calculate governance participation rates for historical analysis.
 *
 * Example:
 * - GovernanceEligibility shows 27 eligible DReps at epoch 200
 * - ProposalVoteSummary shows 18 DRep votes for a proposal expiring at epoch 200
 * - Participation rate = 18/27 = 66.7%
 */
public record GovernanceEligibility(
    Integer epoch,                  // The epoch for which eligibility is calculated
    Integer eligibleDReps,         // Number of DReps eligible to vote at this epoch (excludes NO_CONFIDENCE/ABSTAIN)
    Integer eligibleSPOs,          // Number of stake pools with delegated stake eligible to vote
    Integer eligibleCCMembers      // Number of Constitutional Committee members eligible at this epoch
) {}
