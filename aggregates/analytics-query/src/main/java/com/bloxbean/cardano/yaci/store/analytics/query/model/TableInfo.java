package com.bloxbean.cardano.yaci.store.analytics.query.model;

import java.util.List;

public record TableInfo(
        String name,
        String description,
        long rowCount,
        String partitionStrategy,
        String partitionColumn,
        DateRange dateRange
) {
    public record DateRange(String min, String max) {}
}
