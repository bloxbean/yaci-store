package com.bloxbean.cardano.yaci.store.analytics.query.model;

import java.util.List;

public record TableInfo(
        String name,
        String description,
        long rowCount,
        String partitionStrategy,
        String partitionColumn,
        DateRange dateRange,
        /** "historical" (exported data only, as of dataAsOf) or "historical+live" (unioned with live PostgreSQL up to the chain tip). */
        String dataScope
) {
    public record DateRange(String min, String max) {}
}
