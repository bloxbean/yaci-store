package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.util.List;

/**
 * Holder statistics for all tokens under a specific policy ID.
 * Includes total token count and limited sample of top tokens by holder count.
 *
 * Used by token-holder-stats-by-policy tool to provide policy-level analysis
 * without overwhelming Claude's context window for large NFT collections.
 */
public record PolicyTokenHolderStats(
    String policyId,
    int totalTokenCount,         // Total unique tokens in this policy
    List<TokenHolderStats> tokens,  // Limited sample of tokens (default: 20)
    String message               // LLM guidance about what's shown vs hidden
) {}