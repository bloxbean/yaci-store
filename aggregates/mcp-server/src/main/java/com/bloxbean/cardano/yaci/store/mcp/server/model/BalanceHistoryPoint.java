package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Single point in a balance history timeline.
 * Represents balance at the end of a specific epoch.
 */
public record BalanceHistoryPoint(
    int epoch,
    long utxoCount,
    BigDecimal totalLovelace
) {}
