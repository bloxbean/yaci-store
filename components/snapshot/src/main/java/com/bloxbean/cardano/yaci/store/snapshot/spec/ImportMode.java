package com.bloxbean.cardano.yaci.store.snapshot.spec;

/** Import execution strategy for {@link RestoreMode#IMPORT} tables. */
public enum ImportMode {
    /** Identical source and target column names with compatible types. */
    DIRECT,
    /** Renames, named converters, constants, omissions and target defaults. */
    MAPPED,
    /** One trusted, read-only DuckDB SELECT resource for joins/grouping/reshaping. */
    SQL
}
