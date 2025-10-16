package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

import java.util.List;

/**
 * Response containing database schema information for dynamic queries.
 */
public record SchemaDiscoveryResponse(
    List<TableSchema> tables,
    String documentation
) {}
