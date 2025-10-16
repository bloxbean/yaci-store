package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

/**
 * Represents a field in the SELECT clause with optional aggregation function.
 */
public record AggregationField(
    String column,
    AggregationFunction function,
    String alias
) {
    /**
     * Creates a simple field without aggregation.
     */
    public static AggregationField simple(String column) {
        return new AggregationField(column, null, null);
    }

    /**
     * Creates a field with aggregation function.
     */
    public static AggregationField withFunction(String column, AggregationFunction function, String alias) {
        return new AggregationField(column, function, alias);
    }

    /**
     * Checks if this field has an aggregation function.
     */
    public boolean isAggregated() {
        return function != null;
    }
}
