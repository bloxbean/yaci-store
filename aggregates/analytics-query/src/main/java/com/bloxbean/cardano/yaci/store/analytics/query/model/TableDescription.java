package com.bloxbean.cardano.yaci.store.analytics.query.model;

import java.util.List;

public record TableDescription(
        String table,
        String engine,
        String description,
        long rowCount,
        String partitionStrategy,
        String partitionColumn,
        /** "historical" (exported data only) or "historical+live" (unioned with live PostgreSQL up to the chain tip). */
        String dataScope,
        List<ColumnSchema> columns,
        List<String> queryHints
) {
}
