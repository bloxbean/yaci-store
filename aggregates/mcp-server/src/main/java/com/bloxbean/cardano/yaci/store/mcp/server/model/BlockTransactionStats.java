package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Transaction statistics for a specific block.
 * Provides insights into transaction activity within individual blocks.
 */
public record BlockTransactionStats(
    long blockNumber,
    String blockHash,
    int epoch,
    long slot,
    int txCount,
    BigDecimal totalFees,
    BigDecimal avgFee,
    int validTxCount,
    int invalidTxCount
) {}
