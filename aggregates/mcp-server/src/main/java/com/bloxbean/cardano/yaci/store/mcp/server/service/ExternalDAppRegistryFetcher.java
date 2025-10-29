package com.bloxbean.cardano.yaci.store.mcp.server.service;

import com.bloxbean.cardano.yaci.store.mcp.server.config.DAppRegistryProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service for fetching DApp registry data from external sources (GitHub).
 * Fetches individual DApp JSON files from the Cardano Fans CRFA registry
 * and converts them to DAppEntry objects.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "store.mcp-server.dapp-registry.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ExternalDAppRegistryFetcher {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    // List of known DApp names from the registry
    // This is a curated list of the most important DApps
    // Full list at: https://github.com/Cardano-Fans/crfa-offchain-data-registry/tree/main/dApps
    private static final List<String> KNOWN_DAPPS = Arrays.asList(
        // DEXes
        "Minswap", "SundaeSwap", "Wingriders", "MuesliSwap", "GeniusYield", "SpectrumFinance",
        "TeddySwap", "Meowswap", "VyFinance", "CSWAP",
        // NFT Marketplaces
        "jpgStore", "CNFT_IO", "Tokhun", "nftjam",
        // Lending/DeFi
        "LiqwidFinance", "IndigoProtocol", "Encoins", "AadaFinance", "LendingPond", "OptimFinance",
        // Stablecoins
        "Djed", "USDM", "anetaBTC",
        // NFT Projects
        "SpaceBudz", "ClayNation", "ChilledKongs", "TheApeSociety", "DEADPXLZ", "CardanoWaifus",
        // Infrastructure
        "Hydra", "Meld", "WorldMobileToken", "Iagon", "SingularityNET", "Orcfax", "Charli3"
    );

    /**
     * Fetch all available DApps from the external registry.
     * Only fetches mainnet DApps by default.
     */
    public Map<String, List<DAppRegistryProperties.DAppEntry>> fetchAllDApps(String baseUrl, int timeoutSeconds) {
        Map<String, List<DAppRegistryProperties.DAppEntry>> result = new HashMap<>();
        List<DAppRegistryProperties.DAppEntry> mainnetDApps = new ArrayList<>();

        log.info("Fetching DApps from external registry: {}", baseUrl);
        int successCount = 0;
        int failCount = 0;

        for (String dappName : KNOWN_DAPPS) {
            try {
                Optional<DAppRegistryProperties.DAppEntry> entry = fetchDAppEntry(baseUrl, dappName, timeoutSeconds);
                if (entry.isPresent()) {
                    mainnetDApps.add(entry.get());
                    successCount++;
                    log.debug("Successfully fetched DApp: {}", dappName);
                } else {
                    failCount++;
                    log.warn("Failed to fetch DApp: {}", dappName);
                }
            } catch (Exception e) {
                failCount++;
                log.error("Error fetching DApp {}: {}", dappName, e.getMessage());
            }
        }

        result.put("mainnet", mainnetDApps);
        log.info("External registry fetch complete. Success: {}, Failed: {}", successCount, failCount);

        return result;
    }

    /**
     * Fetch and parse a single DApp entry from GitHub.
     */
    public Optional<DAppRegistryProperties.DAppEntry> fetchDAppEntry(String baseUrl, String dappName, int timeoutSeconds) {
        String url = baseUrl + "/" + dappName + ".json";

        try {
            log.debug("Fetching DApp from: {}", url);

            // Fetch JSON
            String json = restTemplate.getForObject(url, String.class);
            if (json == null || json.trim().isEmpty()) {
                log.warn("Empty response for DApp: {}", dappName);
                return Optional.empty();
            }

            // Parse JSON
            return parseDAppJson(json, dappName);

        } catch (Exception e) {
            log.error("Failed to fetch DApp {} from {}: {}", dappName, url, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parse DApp JSON and extract relevant identifiers.
     */
    private Optional<DAppRegistryProperties.DAppEntry> parseDAppJson(String json, String dappName) {
        try {
            JsonNode root = objectMapper.readTree(json);

            DAppRegistryProperties.DAppEntry entry = new DAppRegistryProperties.DAppEntry();

            // Basic info
            entry.setName(dappName.toLowerCase().replace("_", "."));
            entry.setDisplayName(dappName.replace("_", " "));
            entry.setSourceRegistryId(root.path("subject").asText(dappName));

            // Try to determine category from name/description
            String category = inferCategory(dappName);
            entry.setCategory(category);

            // Description
            String description = root.path("description").path("value").asText("");
            if (description.isEmpty()) {
                description = root.path("description").asText("DApp on Cardano");
            }
            entry.setDescription(description);

            // Extract script addresses and hashes from scripts array
            List<String> scriptAddresses = new ArrayList<>();
            List<String> contractHashes = new ArrayList<>();
            JsonNode scripts = root.path("scripts");
            if (scripts.isArray()) {
                for (JsonNode script : scripts) {
                    // Each script has a versions array
                    JsonNode versions = script.path("versions");
                    if (versions.isArray()) {
                        for (JsonNode version : versions) {
                            // Extract contractAddress (bech32)
                            String address = version.path("contractAddress").asText("");
                            if (address.startsWith("addr")) {
                                scriptAddresses.add(address);
                            }

                            // Extract scriptHash
                            String scriptHash = version.path("scriptHash").asText("");
                            if (!scriptHash.isEmpty() && scriptHash.matches("[0-9a-fA-F]+")) {
                                contractHashes.add(scriptHash);
                            }

                            // Also try fullScriptHash if scriptHash is empty
                            if (scriptHash.isEmpty()) {
                                String fullScriptHash = version.path("fullScriptHash").asText("");
                                if (!fullScriptHash.isEmpty() && fullScriptHash.matches("[0-9a-fA-F]+")) {
                                    contractHashes.add(fullScriptHash);
                                }
                            }
                        }
                    }
                }
            }
            entry.setScriptAddresses(scriptAddresses);
            entry.setContractHashes(contractHashes);

            // Extract policy IDs from mints array
            List<String> policyIds = new ArrayList<>();
            JsonNode mints = root.path("mints");
            if (mints.isArray()) {
                for (JsonNode mint : mints) {
                    // Each mint has a versions array
                    JsonNode versions = mint.path("versions");
                    if (versions.isArray()) {
                        for (JsonNode version : versions) {
                            String policyId = version.path("mintPolicyID").asText("");
                            if (!policyId.isEmpty() && policyId.matches("[0-9a-fA-F]{56}")) {
                                policyIds.add(policyId);
                            }
                        }
                    }
                }
            }
            entry.setPolicyIds(policyIds);

            log.debug("Parsed DApp {}: {} addresses, {} policies, {} hashes",
                     dappName, scriptAddresses.size(), policyIds.size(), contractHashes.size());

            return Optional.of(entry);

        } catch (Exception e) {
            log.error("Failed to parse DApp JSON for {}: {}", dappName, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Infer category from DApp name.
     */
    private String inferCategory(String dappName) {
        String lower = dappName.toLowerCase();

        // DEXes
        if (lower.contains("swap") || lower.contains("dex") || lower.equals("minswap") ||
            lower.equals("geniusyield") || lower.equals("vyfinance") || lower.equals("spectrum")) {
            return "DEX";
        }

        // NFT Marketplaces
        if (lower.contains("jpg") || lower.contains("cnft") || lower.contains("tokhun") ||
            lower.contains("nftjam") || lower.contains("artifct")) {
            return "NFT Marketplace";
        }

        // Lending
        if (lower.contains("lend") || lower.contains("liqwid") || lower.contains("indigo") ||
            lower.contains("aada") || lower.contains("optim")) {
            return "Lending";
        }

        // Stablecoins
        if (lower.contains("djed") || lower.contains("usdm") || lower.contains("aneta")) {
            return "Stablecoin";
        }

        // NFT Collections
        if (lower.contains("spacebudz") || lower.contains("clay") || lower.contains("kong") ||
            lower.contains("ape") || lower.contains("deadpxlz") || lower.contains("waifu")) {
            return "NFT Collection";
        }

        // Infrastructure/Oracles
        if (lower.contains("hydra") || lower.contains("oracle") || lower.contains("orcfax") ||
            lower.contains("charli3") || lower.contains("pigs")) {
            return "Infrastructure";
        }

        return "DApp";
    }
}
