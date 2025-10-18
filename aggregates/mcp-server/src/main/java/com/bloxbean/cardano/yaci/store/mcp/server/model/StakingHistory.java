package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigInteger;
import java.util.List;

/**
 * Model representing complete staking history for a stake address.
 * Includes delegation events and cumulative rewards.
 * All reward amounts in lovelace (BigInteger) to prevent LLM confusion.
 */
public record StakingHistory(
    String stakeAddress,                    // Stake address (stake1... or stake_test...)
    String currentPoolId,                   // Current pool delegation (null if not delegated)
    List<DelegationEvent> delegationHistory, // Chronological list of delegation changes
    BigInteger totalRewardsLovelace,        // Total rewards earned (lovelace)
    Integer firstDelegationEpoch,           // First epoch delegated
    Integer lastDelegationEpoch,            // Most recent delegation epoch
    Integer delegationCount,                // Number of times delegated
    boolean isRegistered                    // Whether stake address is currently registered
) {
    /**
     * Create StakingHistory with calculated metrics.
     */
    public static StakingHistory create(
        String stakeAddress,
        String currentPoolId,
        List<DelegationEvent> delegationHistory,
        BigInteger totalRewardsLovelace,
        boolean isRegistered
    ) {
        Integer firstEpoch = delegationHistory.isEmpty() ? null
            : delegationHistory.get(0).epoch();
        Integer lastEpoch = delegationHistory.isEmpty() ? null
            : delegationHistory.get(delegationHistory.size() - 1).epoch();
        Integer delegationCount = delegationHistory.size();

        return new StakingHistory(
            stakeAddress,
            currentPoolId,
            delegationHistory,
            totalRewardsLovelace,
            firstEpoch,
            lastEpoch,
            delegationCount,
            isRegistered
        );
    }
}
