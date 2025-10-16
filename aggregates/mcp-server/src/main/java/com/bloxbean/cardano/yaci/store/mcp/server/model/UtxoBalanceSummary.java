package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Summary of UTXO balance for one or more addresses.
 * Includes total lovelace, UTXO count, and activity metrics.
 */
public record UtxoBalanceSummary(
    long utxoCount,
    BigDecimal totalLovelace,
    int activeEpochs,
    long firstSeenSlot,
    long lastSeenSlot
) {}
