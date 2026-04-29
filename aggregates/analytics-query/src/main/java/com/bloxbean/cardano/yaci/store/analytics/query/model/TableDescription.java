package com.bloxbean.cardano.yaci.store.analytics.query.model;

import java.util.List;

public record TableDescription(
        String table,
        String engine,
        String description,
        long rowCount,
        String partitionStrategy,
        String partitionColumn,
        List<ColumnSchema> columns,
        List<String> queryHints
) {
}
