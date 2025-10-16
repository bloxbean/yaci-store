package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

import java.util.List;

/**
 * Represents a JOIN specification for multi-table queries.
 * Supports both simple single-condition JOINs and complex multi-condition JOINs.
 *
 * Simple JOIN: JOIN tx ON id = tx.id
 * Composite JOIN: JOIN tx_input ON tx_hash = tx_input.tx_hash AND output_index = tx_input.output_index
 */
public record JoinSpec(
    String joinTable,
    JoinType joinType,
    String leftColumn,      // Legacy: single condition
    String rightColumn,     // Legacy: single condition
    List<JoinCondition> conditions  // New: multiple conditions (for composite keys)
) {
    // Constructor for backward compatibility (single condition)
    public JoinSpec(String joinTable, JoinType joinType, String leftColumn, String rightColumn) {
        this(joinTable, joinType, leftColumn, rightColumn, null);
    }

    // Constructor for multi-condition JOINs
    public JoinSpec(String joinTable, JoinType joinType, List<JoinCondition> conditions) {
        this(joinTable, joinType, null, null, conditions);
    }

    public static JoinSpec inner(String joinTable, String leftColumn, String rightColumn) {
        return new JoinSpec(joinTable, JoinType.INNER, leftColumn, rightColumn);
    }

    public static JoinSpec left(String joinTable, String leftColumn, String rightColumn) {
        return new JoinSpec(joinTable, JoinType.LEFT, leftColumn, rightColumn);
    }

    public static JoinSpec left(String joinTable, List<JoinCondition> conditions) {
        return new JoinSpec(joinTable, JoinType.LEFT, conditions);
    }
}
