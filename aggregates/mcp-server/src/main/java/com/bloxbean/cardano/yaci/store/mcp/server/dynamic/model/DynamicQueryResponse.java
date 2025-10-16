package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

import java.util.List;
import java.util.Map;

/**
 * Response model for dynamic aggregation queries.
 * Contains query results and metadata.
 */
public record DynamicQueryResponse(
    List<Map<String, Object>> rows,
    long totalRows,
    long executionTimeMs,
    Map<String, String> columnTypes,
    String generatedSql
) {
    /**
     * Checks if the query returned any results.
     */
    public boolean hasResults() {
        return rows != null && !rows.isEmpty();
    }

    /**
     * Gets the first row if available.
     */
    public Map<String, Object> getFirstRow() {
        return hasResults() ? rows.get(0) : null;
    }
}
