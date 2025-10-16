package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

/**
 * Supported aggregation functions for dynamic queries.
 * Each function corresponds to a SQL aggregation operation.
 */
public enum AggregationFunction {
    /** Sum of values */
    SUM,

    /** Average of values */
    AVG,

    /** Count of rows */
    COUNT,

    /** Count of distinct values */
    COUNT_DISTINCT,

    /** Minimum value */
    MIN,

    /** Maximum value */
    MAX,

    /** 50th percentile (median) */
    PERCENTILE_50,

    /** 90th percentile */
    PERCENTILE_90,

    /** 95th percentile */
    PERCENTILE_95,

    /** 99th percentile */
    PERCENTILE_99,

    /** Standard deviation */
    STDDEV,

    /** Variance */
    VARIANCE,

    /** JSONB array length - returns number of elements in a JSONB array */
    JSONB_ARRAY_LENGTH
}
