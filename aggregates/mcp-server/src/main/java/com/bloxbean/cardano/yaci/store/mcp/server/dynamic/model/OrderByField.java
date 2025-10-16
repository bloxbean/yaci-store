package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

/**
 * Represents a field in ORDER BY clause.
 */
public record OrderByField(
    String column,
    SortDirection direction
) {
    public static OrderByField asc(String column) {
        return new OrderByField(column, SortDirection.ASC);
    }

    public static OrderByField desc(String column) {
        return new OrderByField(column, SortDirection.DESC);
    }
}
