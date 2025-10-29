package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.mcp.server.config.DAppRegistryProperties;
import com.bloxbean.cardano.yaci.store.mcp.server.model.DAppInfo;
import com.bloxbean.cardano.yaci.store.mcp.server.model.DAppSearchResult;
import com.bloxbean.cardano.yaci.store.mcp.server.model.DAppSummary;
import com.bloxbean.cardano.yaci.store.mcp.server.service.ExternalDAppRegistryFetcher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP service for DApp registry lookups.
 * Enables LLMs to:
 * - Resolve DApp names to on-chain identifiers (addresses, policy IDs)
 * - Identify DApps from addresses (reverse lookup)
 * - Browse DApps by category
 * - Discover available DApps in the Cardano ecosystem
 *
 * Data source: Configured in application.yml under store.mcp-server.dapp-registry
 * Based on: Cardano Fans CRFA off-chain data registry
 */
@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
@ConditionalOnProperty(
    name = "store.mcp-server.dapp-registry.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class McpDAppRegistryService {
    private final DAppRegistryProperties registryProperties;
    private final StoreProperties storeProperties;

    @Autowired(required = false)
    private ExternalDAppRegistryFetcher externalFetcher;

    // In-memory indices for fast lookups
    private volatile List<DAppInfo> allDApps = new ArrayList<>();
    private volatile Map<String, List<DAppInfo>> dappsByNetwork = new HashMap<>();
    private volatile Map<String, DAppInfo> dappsByAddress = new HashMap<>();      // address → DApp
    private volatile Map<String, DAppInfo> dappsByPolicy = new HashMap<>();       // policyId → DApp
    private volatile Map<String, DAppInfo> dappsByContractHash = new HashMap<>(); // hash → DApp
    private volatile Map<String, List<DAppInfo>> dappsByCategory = new HashMap<>(); // category → DApps

    // External registry tracking
    private volatile long lastSuccessfulSync = 0;
    private volatile int externalDAppsCount = 0;
    private volatile int localDAppsCount = 0;

    private String currentNetwork;

    @PostConstruct
    public void init() {
        log.info("Initializing DApp registry...");

        // Determine current network
        long protocolMagic = storeProperties.getProtocolMagic();
        NetworkType networkType = NetworkType.fromProtocolMagic(protocolMagic);
        currentNetwork = networkType.name().toLowerCase();

        log.info("Current network: {}", currentNetwork);

        // Load and index DApps from local YAML
        loadAndIndexDApps();
        localDAppsCount = allDApps.size();

        log.info("DApp registry initialized with {} local DApps for network {}",
                 localDAppsCount, currentNetwork);

        // Initial sync from external registry if enabled
        if (registryProperties.getExternalRegistry().isEnabled() && externalFetcher != null) {
            log.info("External registry sync is enabled. Triggering initial sync...");
            syncExternalRegistry();
        } else {
            log.info("External registry sync is disabled");
        }
    }

    private void loadAndIndexDApps() {
        Map<String, List<DAppRegistryProperties.DAppEntry>> dappsConfig = registryProperties.getDapps();

        // Process each network
        for (Map.Entry<String, List<DAppRegistryProperties.DAppEntry>> entry : dappsConfig.entrySet()) {
            String network = entry.getKey();
            List<DAppRegistryProperties.DAppEntry> entries = entry.getValue();

            List<DAppInfo> networkDApps = new ArrayList<>();

            for (DAppRegistryProperties.DAppEntry entry1 : entries) {
                DAppInfo dapp = new DAppInfo(
                    entry1.getName(),
                    entry1.getDisplayName(),
                    entry1.getCategory(),
                    entry1.getDescription(),
                    entry1.getScriptAddresses(),
                    entry1.getPolicyIds(),
                    entry1.getContractHashes(),
                    network,
                    entry1.getSourceRegistryId()
                );

                networkDApps.add(dapp);
                allDApps.add(dapp);

                // Build reverse lookup indices
                for (String address : dapp.scriptAddresses()) {
                    dappsByAddress.put(address.toLowerCase(), dapp);
                }
                for (String policyId : dapp.policyIds()) {
                    dappsByPolicy.put(policyId.toLowerCase(), dapp);
                }
                for (String hash : dapp.contractHashes()) {
                    dappsByContractHash.put(hash.toLowerCase(), dapp);
                }

                // Build category index
                String category = dapp.category();
                dappsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(dapp);
            }

            dappsByNetwork.put(network, networkDApps);
        }
    }

    @Tool(name = "dapp-lookup",
          description = "🏷️ Look up well-known DApp information by name (e.g., 'minswap', 'sundaeswap', 'jpg.store'). " +
                       "Returns script addresses, policy IDs, and contract hashes for the DApp. " +
                       "CRITICAL for: " +
                       "- Resolving DApp names to addresses for querying (e.g., 'show minswap TVL') " +
                       "- Tagging addresses with known DApp names in results " +
                       "- Finding DApp tokens by policy ID " +
                       "Supports partial name matching (case-insensitive). " +
                       "Automatically uses current network context.")
    public DAppSearchResult lookupDApp(
        @ToolParam(description = "DApp name to search (e.g., 'minswap', 'sundae', 'jpg')") String name
    ) {
        log.debug("Looking up DApp: {}", name);

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("DApp name cannot be empty");
        }

        String searchTerm = name.trim().toLowerCase();

        // Get DApps for current network
        List<DAppInfo> networkDApps = dappsByNetwork.getOrDefault(currentNetwork, new ArrayList<>());

        // Exact matches (case-insensitive)
        List<DAppInfo> exactMatches = networkDApps.stream()
            .filter(dapp -> dapp.name().equalsIgnoreCase(searchTerm))
            .collect(Collectors.toList());

        // Partial matches (name contains search term)
        List<DAppInfo> partialMatches = networkDApps.stream()
            .filter(dapp -> !dapp.name().equalsIgnoreCase(searchTerm)) // Exclude exact matches
            .filter(dapp -> dapp.name().toLowerCase().contains(searchTerm) ||
                           dapp.displayName().toLowerCase().contains(searchTerm) ||
                           dapp.description().toLowerCase().contains(searchTerm))
            .collect(Collectors.toList());

        return DAppSearchResult.create(exactMatches, partialMatches, name, currentNetwork);
    }

    @Tool(name = "dapp-list-by-category",
          description = "📋 List known DApps in a specific category (lightweight summary). " +
                       "Categories: DEX, NFT Marketplace, Lending, NFT Collection, Wallet, Identity, Gaming, AI, etc. " +
                       "Use 'All' or empty string to see all categories. " +
                       "Returns only name, display name, and category (no addresses/policy IDs). " +
                       "⚠️ Limited to 10 random DApps if more exist (to prevent token overflow). " +
                       "Use 'dapp-lookup' to get full details for a specific DApp. " +
                       "Automatically filters by current network.")
    public List<DAppSummary> listDAppsByCategory(
        @ToolParam(description = "Category: 'DEX', 'NFT Marketplace', 'Lending', 'All', etc. (default: 'All')") String category
    ) {
        log.debug("Listing DApps by category: {}", category);

        List<DAppInfo> networkDApps = dappsByNetwork.getOrDefault(currentNetwork, new ArrayList<>());

        // Filter by category
        List<DAppInfo> filtered;
        if (category == null || category.trim().isEmpty() || category.equalsIgnoreCase("All")) {
            filtered = new ArrayList<>(networkDApps);
        } else {
            String searchCategory = category.trim();
            filtered = networkDApps.stream()
                .filter(dapp -> dapp.category().equalsIgnoreCase(searchCategory))
                .collect(Collectors.toList());
        }

        // Limit to 10 random items if too many
        int originalSize = filtered.size();
        if (originalSize > 10) {
            Collections.shuffle(filtered);
            filtered = filtered.subList(0, 10);
            log.info("Limited DApp list to 10 random items from {} total for category '{}'",
                    originalSize, category != null ? category : "All");
        }

        // Convert to lightweight summary
        return filtered.stream()
            .map(dapp -> new DAppSummary(dapp.name(), dapp.displayName(), dapp.category()))
            .collect(Collectors.toList());
    }

    @Tool(name = "dapp-reverse-lookup",
          description = "🔍 Identify DApp from script address, policy ID, or contract hash. " +
                       "CRITICAL for tagging addresses in query results. " +
                       "When you see an address in transaction/UTXO results, use this to check if it's a known DApp. " +
                       "Example: addr1z8snz7c4... → 'Minswap DEX' " +
                       "Returns DApp info if found, null if unknown. " +
                       "Supports both full addresses and partial identifiers.")
    public DAppInfo reverseLookupDApp(
        @ToolParam(description = "Address (bech32), policy ID (hex), or contract hash (hex) to identify") String identifier
    ) {
        log.debug("Reverse lookup for identifier: {}", identifier);

        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be empty");
        }

        String searchId = identifier.trim().toLowerCase();

        // Try exact match first
        DAppInfo dapp = dappsByAddress.get(searchId);
        if (dapp != null && dapp.network().equals(currentNetwork)) {
            return dapp;
        }

        dapp = dappsByPolicy.get(searchId);
        if (dapp != null && dapp.network().equals(currentNetwork)) {
            return dapp;
        }

        dapp = dappsByContractHash.get(searchId);
        if (dapp != null && dapp.network().equals(currentNetwork)) {
            return dapp;
        }

        // Try partial match (for addresses that might be prefixes)
        List<DAppInfo> networkDApps = dappsByNetwork.getOrDefault(currentNetwork, new ArrayList<>());
        for (DAppInfo d : networkDApps) {
            // Check script addresses
            for (String address : d.scriptAddresses()) {
                if (address.toLowerCase().contains(searchId) || searchId.contains(address.toLowerCase())) {
                    return d;
                }
            }
            // Check policy IDs
            for (String policyId : d.policyIds()) {
                if (policyId.toLowerCase().contains(searchId) || searchId.contains(policyId.toLowerCase())) {
                    return d;
                }
            }
            // Check contract hashes
            for (String hash : d.contractHashes()) {
                if (hash.toLowerCase().contains(searchId) || searchId.contains(hash.toLowerCase())) {
                    return d;
                }
            }
        }

        log.debug("No DApp found for identifier: {}", identifier);
        return null;
    }

    @Tool(name = "dapp-list-all",
          description = "📚 List all known DApps in the registry for current network (lightweight summary). " +
                       "Returns only name, display name, and category for each DApp (no addresses/policy IDs). " +
                       "⚠️ Limited to 10 random DApps if more exist (to prevent token overflow). " +
                       "Use 'dapp-lookup' to get full details (addresses, policy IDs) for a specific DApp. " +
                       "Use 'dapp-list-by-category' to filter by category. " +
                       "Available categories: DEX, NFT Marketplace, Lending, NFT Collection, AI, etc.")
    public List<DAppSummary> listAllDApps() {
        log.debug("Listing all DApps for network: {}", currentNetwork);

        List<DAppInfo> networkDApps = new ArrayList<>(
            dappsByNetwork.getOrDefault(currentNetwork, new ArrayList<>())
        );

        // Limit to 10 random items if too many
        int originalSize = networkDApps.size();
        if (originalSize > 10) {
            Collections.shuffle(networkDApps);
            networkDApps = networkDApps.subList(0, 10);
            log.info("Limited DApp list to 10 random items from {} total DApps", originalSize);
        }

        // Convert to lightweight summary
        return networkDApps.stream()
            .map(dapp -> new DAppSummary(dapp.name(), dapp.displayName(), dapp.category()))
            .collect(Collectors.toList());
    }

    /**
     * Get current network name for external use.
     */
    public String getCurrentNetwork() {
        return currentNetwork;
    }

    /**
     * Get statistics about the registry.
     */
    public String getRegistryStats() {
        int totalDApps = allDApps.size();
        int currentNetworkDApps = dappsByNetwork.getOrDefault(currentNetwork, new ArrayList<>()).size();
        int categories = dappsByCategory.size();

        return String.format("Registry stats - Network: %s, DApps: %d/%d, Categories: %d",
            currentNetwork, currentNetworkDApps, totalDApps, categories);
    }

    /**
     * Scheduled sync from external GitHub registry.
     * Runs daily at 2 AM by default (configurable via cron expression).
     */
    @Scheduled(cron = "${store.mcp-server.dapp-registry.external-registry.schedule:0 0 2 * * ?}")
    public void syncExternalRegistry() {
        if (!registryProperties.getExternalRegistry().isEnabled()) {
            log.debug("External registry sync is disabled");
            return;
        }

        if (externalFetcher == null) {
            log.warn("ExternalDAppRegistryFetcher not available, skipping sync");
            return;
        }

        log.info("Starting scheduled DApp registry sync from GitHub...");

        try {
            String baseUrl = registryProperties.getExternalRegistry().getUrl();
            int timeout = registryProperties.getExternalRegistry().getTimeoutSeconds();

            // Fetch external DApps
            Map<String, List<DAppRegistryProperties.DAppEntry>> externalDApps =
                externalFetcher.fetchAllDApps(baseUrl, timeout);

            if (externalDApps == null || externalDApps.isEmpty()) {
                log.warn("No external DApps fetched from registry");
                return;
            }

            // Merge with local YAML entries (local takes precedence)
            if (registryProperties.getExternalRegistry().isAutoMerge()) {
                mergeExternalDApps(externalDApps);
            }

            lastSuccessfulSync = Instant.now().getEpochSecond();
            log.info("DApp registry sync completed successfully. " +
                    "Total DApps: {}, Local: {}, External: {}, Last Sync: {}",
                    allDApps.size(), localDAppsCount, externalDAppsCount,
                    Instant.ofEpochSecond(lastSuccessfulSync));

        } catch (Exception e) {
            log.error("Failed to sync DApp registry from external source: {}", e.getMessage(), e);
            if (!registryProperties.getExternalRegistry().isFailSilently()) {
                throw new RuntimeException("DApp registry sync failed", e);
            }
        }
    }

    /**
     * Merge external DApps with local YAML entries.
     * Local YAML entries take precedence over external entries.
     *
     * Strategy:
     * 1. Start with local YAML DApps (already loaded)
     * 2. Add external DApps that don't exist in local YAML
     * 3. Rebuild all indices
     */
    private void mergeExternalDApps(Map<String, List<DAppRegistryProperties.DAppEntry>> externalDApps) {
        log.info("Merging external DApps with local YAML entries...");

        // Get local DApp names for current network (already loaded in init)
        Set<String> localDAppNames = dappsByNetwork.getOrDefault(currentNetwork, new ArrayList<>())
            .stream()
            .map(DAppInfo::name)
            .collect(Collectors.toSet());

        log.debug("Local DApp names for {}: {}", currentNetwork, localDAppNames);

        // Process external DApps for current network
        List<DAppRegistryProperties.DAppEntry> externalEntries =
            externalDApps.getOrDefault(currentNetwork, new ArrayList<>());

        List<DAppInfo> newExternalDApps = new ArrayList<>();
        int skipCount = 0;

        for (DAppRegistryProperties.DAppEntry entry : externalEntries) {
            // Skip if already exists in local YAML
            if (localDAppNames.contains(entry.getName())) {
                skipCount++;
                log.debug("Skipping external DApp '{}' - exists in local YAML", entry.getName());
                continue;
            }

            // Convert to DAppInfo and add
            DAppInfo dapp = new DAppInfo(
                entry.getName(),
                entry.getDisplayName(),
                entry.getCategory(),
                entry.getDescription(),
                entry.getScriptAddresses(),
                entry.getPolicyIds(),
                entry.getContractHashes(),
                currentNetwork,
                entry.getSourceRegistryId()
            );

            newExternalDApps.add(dapp);
        }

        externalDAppsCount = newExternalDApps.size();
        log.info("Merged {} new external DApps (skipped {} existing local entries)",
                externalDAppsCount, skipCount);

        // Add to allDApps
        allDApps.addAll(newExternalDApps);

        // Add to network-specific list
        List<DAppInfo> networkDApps = dappsByNetwork.getOrDefault(currentNetwork, new ArrayList<>());
        networkDApps.addAll(newExternalDApps);
        dappsByNetwork.put(currentNetwork, networkDApps);

        // Rebuild indices for new DApps
        for (DAppInfo dapp : newExternalDApps) {
            // Address index
            for (String address : dapp.scriptAddresses()) {
                dappsByAddress.put(address.toLowerCase(), dapp);
            }
            // Policy ID index
            for (String policyId : dapp.policyIds()) {
                dappsByPolicy.put(policyId.toLowerCase(), dapp);
            }
            // Contract hash index
            for (String hash : dapp.contractHashes()) {
                dappsByContractHash.put(hash.toLowerCase(), dapp);
            }
            // Category index
            String category = dapp.category();
            dappsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(dapp);
        }

        log.info("DApp registry merge complete. Total DApps: {}", allDApps.size());
    }

    @Tool(name = "dapp-registry-status",
          description = "📊 Get DApp registry status and statistics. " +
                       "Shows total DApps loaded, local vs external counts, last sync time, and categories. " +
                       "Useful for debugging or verifying registry state. " +
                       "Returns sync status, counts, and last successful sync timestamp.")
    public Map<String, Object> getRegistryStatus() {
        Map<String, Object> status = new HashMap<>();

        // Basic counts
        status.put("totalDApps", allDApps.size());
        status.put("localDApps", localDAppsCount);
        status.put("externalDApps", externalDAppsCount);
        status.put("currentNetwork", currentNetwork);

        // Network breakdown
        Map<String, Integer> networkCounts = new HashMap<>();
        for (Map.Entry<String, List<DAppInfo>> entry : dappsByNetwork.entrySet()) {
            networkCounts.put(entry.getKey(), entry.getValue().size());
        }
        status.put("dappsByNetwork", networkCounts);

        // Category breakdown
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (Map.Entry<String, List<DAppInfo>> entry : dappsByCategory.entrySet()) {
            categoryCounts.put(entry.getKey(), entry.getValue().size());
        }
        status.put("dappsByCategory", categoryCounts);

        // External sync status
        status.put("externalSyncEnabled", registryProperties.getExternalRegistry().isEnabled());
        if (lastSuccessfulSync > 0) {
            status.put("lastSuccessfulSync", Instant.ofEpochSecond(lastSuccessfulSync).toString());
            status.put("lastSyncTimestamp", lastSuccessfulSync);
        } else {
            status.put("lastSuccessfulSync", "Never");
            status.put("lastSyncTimestamp", 0);
        }

        // Index sizes
        status.put("addressIndexSize", dappsByAddress.size());
        status.put("policyIndexSize", dappsByPolicy.size());
        status.put("contractHashIndexSize", dappsByContractHash.size());

        return status;
    }
}
