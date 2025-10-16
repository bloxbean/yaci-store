package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.config;

import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.AggregationFunction;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.FilterOperator;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Whitelist of allowed operations (aggregations and filters) for dynamic queries.
 */
@Component
public class OperationWhitelist {

    // Allowed aggregation functions
    private static final Set<AggregationFunction> ALLOWED_AGGREGATIONS = Set.of(
        AggregationFunction.SUM,
        AggregationFunction.AVG,
        AggregationFunction.COUNT,
        AggregationFunction.COUNT_DISTINCT,
        AggregationFunction.MIN,
        AggregationFunction.MAX,
        AggregationFunction.PERCENTILE_50,
        AggregationFunction.PERCENTILE_90,
        AggregationFunction.PERCENTILE_95,
        AggregationFunction.PERCENTILE_99,
        AggregationFunction.JSONB_ARRAY_LENGTH
    );

    // Allowed filter operators
    private static final Set<FilterOperator> ALLOWED_OPERATORS = Set.of(
        FilterOperator.EQ,
        FilterOperator.NE,
        FilterOperator.GT,
        FilterOperator.LT,
        FilterOperator.GTE,
        FilterOperator.LTE,
        FilterOperator.IN,
        FilterOperator.BETWEEN,
        FilterOperator.IS_NULL,
        FilterOperator.IS_NOT_NULL,
        FilterOperator.JSONB_ARRAY_LENGTH_EQ,
        FilterOperator.JSONB_ARRAY_LENGTH_GT,
        FilterOperator.JSONB_ARRAY_LENGTH_GTE,
        FilterOperator.JSONB_ARRAY_LENGTH_LT,
        FilterOperator.JSONB_ARRAY_LENGTH_LTE,
        FilterOperator.JSONB_CONTAINS,
        FilterOperator.JSONB_PATH_MATCH
    );

    /**
     * Checks if an aggregation function is allowed.
     */
    public boolean isAggregationAllowed(AggregationFunction function) {
        return ALLOWED_AGGREGATIONS.contains(function);
    }

    /**
     * Checks if a filter operator is allowed.
     */
    public boolean isOperatorAllowed(FilterOperator operator) {
        return ALLOWED_OPERATORS.contains(operator);
    }

    /**
     * Gets all allowed aggregation functions.
     */
    public Set<AggregationFunction> getAllowedAggregations() {
        return ALLOWED_AGGREGATIONS;
    }

    /**
     * Gets all allowed filter operators.
     */
    public Set<FilterOperator> getAllowedOperators() {
        return ALLOWED_OPERATORS;
    }
}
