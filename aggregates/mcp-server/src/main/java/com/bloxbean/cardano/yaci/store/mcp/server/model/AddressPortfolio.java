package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Model representing complete address portfolio with ADA and native tokens.
 * Returns BOTH lovelace (BigInteger) and ADA (BigDecimal) to prevent LLM confusion.
 */
public record AddressPortfolio(
    String address,                  // Address (addr_test... or addr... format)
    BigInteger totalAdaLovelace,     // Total ADA in lovelace (whole units)
    BigDecimal totalAdaAda,          // Total ADA in ADA units (with decimals for readability)
    List<TokenBalance> nativeTokens, // Native tokens held (excludes lovelace/ADA, max 50)
    int nftCount,                    // Count of NFTs (tokens with quantity=1)
    int utxoCount,                   // Number of unspent UTXOs
    int totalTokenCount,             // Total unique tokens (before limiting to 50)
    String message                   // Information for LLM (e.g., if tokens limited or skipped)
) {}
