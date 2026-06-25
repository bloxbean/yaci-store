package com.bloxbean.cardano.yaci.store.analytics.query.model;

import java.util.List;
import java.util.Map;

public record SchemaOverview(
        String engine,
        String sqlDialect,
        int dataStalnessDays,
        String dataAsOf,
        String note,
        List<TableInfo> tables,
        Map<String, String> queryHints
) {
}
