package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.validation;

import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.DynamicQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates query complexity to prevent resource exhaustion.
 * Enforces limits on joins, filters, result size, etc.
 */
@Component
@Slf4j
public class QueryComplexityValidator {

    @Value("${store.mcp-server.aggregation.dynamic-query.max-filters:20}")
    private int maxFilterConditions;

    @Value("${store.mcp-server.aggregation.dynamic-query.max-joins:3}")
    private int maxJoins;

    @Value("${store.mcp-server.aggregation.dynamic-query.max-group-by:10}")
    private int maxGroupByColumns;

    @Value("${store.mcp-server.aggregation.dynamic-query.max-result-size:10000}")
    private int maxResultRows;

    @Value("${store.mcp-server.aggregation.dynamic-query.max-select-fields:50}")
    private int maxSelectFields = 50;

    /**
     * Validates query complexity.
     * Throws IllegalArgumentException if complexity limits are exceeded.
     */
    public void validateComplexity(DynamicQueryRequest request) {
        // Validate number of SELECT fields
        if (request.selectFields().size() > maxSelectFields) {
            throw new IllegalArgumentException(
                "Too many SELECT fields: " + request.selectFields().size() +
                " (max: " + maxSelectFields + ")"
            );
        }

        // Validate number of filter conditions
        int totalFilters = request.filters().size() + request.having().size();
        if (totalFilters > maxFilterConditions) {
            throw new IllegalArgumentException(
                "Too many filter conditions: " + totalFilters +
                " (max: " + maxFilterConditions + ")"
            );
        }

        // Validate number of joins
        if (request.joins().size() > maxJoins) {
            throw new IllegalArgumentException(
                "Too many joins: " + request.joins().size() +
                " (max: " + maxJoins + ")"
            );
        }

        // Validate number of GROUP BY columns
        if (request.groupBy().size() > maxGroupByColumns) {
            throw new IllegalArgumentException(
                "Too many GROUP BY columns: " + request.groupBy().size() +
                " (max: " + maxGroupByColumns + ")"
            );
        }

        // Validate result limit
        if (request.limit() != null && request.limit() > maxResultRows) {
            throw new IllegalArgumentException(
                "Result limit too high: " + request.limit() +
                " (max: " + maxResultRows + ")"
            );
        }

        // If no limit specified, enforce default max
        if (request.limit() == null) {
            log.warn("No LIMIT specified, enforcing default max: {}", maxResultRows);
        }
    }

    /**
     * Gets the effective limit for a query (enforces max if not specified).
     */
    public int getEffectiveLimit(DynamicQueryRequest request) {
        if (request.limit() == null) {
            return maxResultRows;
        }
        return Math.min(request.limit(), maxResultRows);
    }
}
