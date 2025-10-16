package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

/**
 * Common query pattern with example.
 */
public record QueryPattern(
    String description,
    String hint,
    String example
) {}
