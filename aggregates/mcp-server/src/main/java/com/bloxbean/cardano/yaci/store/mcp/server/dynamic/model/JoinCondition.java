package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

/**
 * Represents a single join condition (column equality) in a JOIN clause.
 * Used for composite JOINs that require multiple column matches.
 *
 * Example: JOIN tx_input ON address_utxo.tx_hash = tx_input.tx_hash
 *                        AND address_utxo.output_index = tx_input.output_index
 */
public record JoinCondition(
    String leftColumn,
    String rightColumn
) {}
