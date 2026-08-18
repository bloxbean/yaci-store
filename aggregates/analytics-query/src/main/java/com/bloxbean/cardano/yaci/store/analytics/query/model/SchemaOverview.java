package com.bloxbean.cardano.yaci.store.analytics.query.model;

import java.util.List;
import java.util.Map;

public record SchemaOverview(
        String engine,
        String sqlDialect,
        int dataStalnessDays,
        String dataAsOf,
        /** True when live PostgreSQL federation is active: tables with dataScope "historical+live" reach the chain tip. */
        boolean liveDataActive,
        String note,
        List<TableInfo> tables,
        Map<String, String> queryHints
) {
}
