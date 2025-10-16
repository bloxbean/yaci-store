package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

/**
 * Represents a field in GROUP BY clause.
 */
public record GroupByField(
    String column
) {
    public static GroupByField of(String column) {
        return new GroupByField(column);
    }
}
