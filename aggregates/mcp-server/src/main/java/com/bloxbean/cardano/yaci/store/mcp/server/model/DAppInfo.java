package com.bloxbean.cardano.yaci.store.mcp.server.model;

import java.util.List;

/**
 * Model representing a decentralized application (DApp) on Cardano.
 * Contains on-chain identifiers (addresses, policy IDs, contract hashes) for the DApp.
 *
 * Used by the DApp registry MCP tool to enable:
 * - Name-based lookups (e.g., "minswap" → addresses)
 * - Reverse lookups (address → DApp name)
 * - Category-based browsing (e.g., all DEXes)
 * - DApp identification and tagging in query results
 */
public record DAppInfo(
    String name,                       // Lowercase identifier (e.g., "minswap", "jpg.store")
    String displayName,                // Human-readable name (e.g., "Minswap", "JPG Store")
    String category,                   // Category: "DEX", "NFT Marketplace", "Lending", etc.
    String description,                // Brief description of the DApp
    List<String> scriptAddresses,      // List of known script addresses (bech32 format)
    List<String> policyIds,            // List of asset policy IDs (hex format)
    List<String> contractHashes,       // List of contract hashes (hex format)
    String network,                    // Network: "mainnet", "preprod", "preview"
    String sourceRegistryId            // Optional: Reference ID from source registry
) {}
