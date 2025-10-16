package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

/**
 * Represents a filter condition in WHERE or HAVING clause.
 */
public record FilterCondition(
    String column,
    FilterOperator operator,
    Object value,
    LogicalOperator logicalOp
) {
    /**
     * Creates a simple filter with AND logic.
     */
    public static FilterCondition and(String column, FilterOperator operator, Object value) {
        return new FilterCondition(column, operator, value, LogicalOperator.AND);
    }

    /**
     * Creates a filter with OR logic.
     */
    public static FilterCondition or(String column, FilterOperator operator, Object value) {
        return new FilterCondition(column, operator, value, LogicalOperator.OR);
    }

    /**
     * Checks if this filter requires a value.
     */
    public boolean requiresValue() {
        return operator != FilterOperator.IS_NULL && operator != FilterOperator.IS_NOT_NULL;
    }
}
