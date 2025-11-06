package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;

/**
 * Represents a holder of a specific asset, ranked by quantity held.
 * Used for top holders / whale analysis queries.
 */
public record AssetHolderRanking(
    int rank,
    String address,
    String assetUnit,
    BigDecimal quantity
) {}