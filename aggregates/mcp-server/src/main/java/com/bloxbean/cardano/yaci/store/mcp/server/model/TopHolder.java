package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Model representing a top holder of a token.
 * Used for whale analysis and concentration metrics.
 */
public record TopHolder(
    String address,                 // Holder address
    BigInteger quantity,            // Token quantity held (whole units)
    BigDecimal percentageOfSupply,  // Percentage of total supply
    int rank                        // Rank by quantity (1 = largest holder)
) {}
