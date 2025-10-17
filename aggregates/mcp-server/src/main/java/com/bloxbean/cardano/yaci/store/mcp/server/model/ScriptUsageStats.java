package com.bloxbean.cardano.yaci.store.mcp.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Statistics for script usage and execution metrics.
 * Provides analytics for smart contract activity including execution counts,
 * unique users, and resource consumption (memory and CPU units).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScriptUsageStats(
    String scriptHash,
    String scriptType,          // NATIVE_SCRIPT, PLUTUS_V1, PLUTUS_V2, PLUTUS_V3
    Long executionCount,        // Total number of times script was executed
    Long uniqueTransactions,    // Number of distinct transactions using this script
    Long avgMemUnits,           // Average memory units per execution
    Long avgCpuUnits,           // Average CPU steps per execution
    Long totalMemUnits,         // Total memory units consumed
    Long totalCpuUnits,         // Total CPU steps consumed
    Long firstSeenSlot,         // First slot where script was executed
    Long lastSeenSlot           // Most recent slot where script was executed
) {
}
