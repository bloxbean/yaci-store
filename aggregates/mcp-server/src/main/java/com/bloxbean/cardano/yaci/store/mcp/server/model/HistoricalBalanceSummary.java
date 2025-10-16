package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Historical balance at a specific point in time.
 * Supports epoch-based and slot-based point-in-time queries.
 */
public record HistoricalBalanceSummary(
    String timeType,        // "epoch", "slot", "block"
    long timeValue,
    long utxoCount,
    BigDecimal totalLovelace
) {}
