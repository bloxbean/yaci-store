package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

/**
 * Schema information for a database column.
 */
public record ColumnSchema(
    String name,
    String type,
    String description
) {}
