package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

/**
 * Supported filter operators for WHERE and HAVING clauses.
 */
public enum FilterOperator {
    /** Equals (=) */
    EQ,

    /** Not equals (!=) */
    NE,

    /** Greater than */
    GT,

    /** Less than */
    LT,

    /** Greater than or equal */
    GTE,

    /** Less than or equal */
    LTE,

    /** IN clause */
    IN,

    /** NOT IN clause */
    NOT_IN,

    /** BETWEEN clause */
    BETWEEN,

    /** LIKE pattern matching */
    LIKE,

    /** NOT LIKE pattern matching */
    NOT_LIKE,

    /** IS NULL check */
    IS_NULL,

    /** IS NOT NULL check */
    IS_NOT_NULL,

    /** JSONB array length equals */
    JSONB_ARRAY_LENGTH_EQ,

    /** JSONB array length greater than */
    JSONB_ARRAY_LENGTH_GT,

    /** JSONB array length greater than or equal */
    JSONB_ARRAY_LENGTH_GTE,

    /** JSONB array length less than */
    JSONB_ARRAY_LENGTH_LT,

    /** JSONB array length less than or equal */
    JSONB_ARRAY_LENGTH_LTE,

    /** JSONB contains - checks if left JSONB contains right JSONB (@>) */
    JSONB_CONTAINS,

    /** JSONB path match - checks if JSONB matches JSONPath expression (@@) */
    JSONB_PATH_MATCH
}
