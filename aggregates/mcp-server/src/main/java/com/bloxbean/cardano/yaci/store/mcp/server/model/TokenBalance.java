package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.math.BigInteger;

/**
 * Model representing a native token balance in an address portfolio.
 * All quantities are in whole units (BigInteger) to avoid LLM confusion about decimals.
 */
public record TokenBalance(
    String unit,                // Full asset unit (policyId + assetName hex)
    String policyId,            // Policy ID (hex)
    String assetName,           // Asset name (UTF-8 decoded if possible)
    String assetNameHex,        // Asset name in hex format
    BigInteger quantity         // Token quantity (whole units only, no decimals)
) {}
