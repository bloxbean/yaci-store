package com.bloxbean.cardano.yaci.store.mcp.server.model;

/**
 * Model representing the current or historical constitution details.
 * Contains constitution metadata and optional guardrail script information.
 */
public record ConstitutionDetails(
    Integer activeEpoch,            // Epoch when this constitution became active
    String anchorUrl,               // URL to constitution document
    String anchorHash,              // Hash of constitution document
    Long slot,                      // Slot when constitution was enacted
    String script,                  // Optional guardrail script (if present)
    Boolean isCurrent               // Whether this is the current active constitution
) {}
