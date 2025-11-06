package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Represents an address ranked by its ADA balance.
 * Used for top addresses / rich list queries.
 */
public record AddressBalanceRanking(
    int rank,
    String address,
    BigDecimal lovelaceBalance,
    BigDecimal adaBalance
) {}