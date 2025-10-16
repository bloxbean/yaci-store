package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

import java.util.List;

/**
 * Schema information for a database table.
 */
public record TableSchema(
    String name,
    String description,
    List<String> primaryKeys,
    List<ColumnSchema> columns,
    List<QueryPattern> commonPatterns
) {}
