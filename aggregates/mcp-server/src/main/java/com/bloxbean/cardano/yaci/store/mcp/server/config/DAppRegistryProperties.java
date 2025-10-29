package com.bloxbean.cardano.yaci.store.mcp.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for the DApp registry.
 * Binds to store.mcp-server.dapp-registry.* in application.yml
 *
 * Example configuration:
 * <pre>
 * store:
 *   mcp-server:
 *     dapp-registry:
 *       enabled: true
 *       dapps:
 *         mainnet:
 *           - name: "minswap"
 *             display-name: "Minswap"
 *             category: "DEX"
 *             description: "Multi-pool AMM DEX"
 *             script-addresses:
 *               - "addr1..."
 *             policy-ids:
 *               - "e16c2dc8..."
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "store.mcp-server.dapp-registry")
@Data
public class DAppRegistryProperties {
    /**
     * Enable/disable the DApp registry MCP tools.
     */
    private boolean enabled = true;

    /**
     * External registry configuration for automatic syncing from GitHub.
     */
    private ExternalRegistry externalRegistry = new ExternalRegistry();

    /**
     * DApp entries organized by network (mainnet, preprod, preview).
     * Each network contains a list of DApp entries.
     */
    private Map<String, List<DAppEntry>> dapps = new HashMap<>();

    /**
     * Configuration for external DApp registry synchronization.
     */
    @Data
    public static class ExternalRegistry {
        /**
         * Enable/disable external registry sync.
         * Default: false (disabled for safety - enable when ready)
         */
        private boolean enabled = false;

        /**
         * Base URL for external DApp registry (GitHub).
         * Default: Cardano Fans CRFA off-chain data registry
         */
        private String url = "https://raw.githubusercontent.com/Cardano-Fans/crfa-offchain-data-registry/main/dApps";

        /**
         * Cron expression for sync schedule.
         * Default: Daily at 2 AM
         */
        private String schedule = "0 0 2 * * ?";

        /**
         * Whether to merge external DApps with local YAML entries.
         * If true, local YAML entries take precedence.
         * Default: true
         */
        private boolean autoMerge = true;

        /**
         * HTTP request timeout in seconds.
         * Default: 30 seconds
         */
        private int timeoutSeconds = 30;

        /**
         * Whether to fail silently on sync errors.
         * If true, errors are logged but don't crash the application.
         * Default: true
         */
        private boolean failSilently = true;
    }

    /**
     * Represents a single DApp entry from configuration.
     */
    @Data
    public static class DAppEntry {
        private String name;                                    // Lowercase identifier
        private String displayName;                             // Human-readable name
        private String category;                                // Category: DEX, NFT Marketplace, etc.
        private String description;                             // Brief description
        private List<String> scriptAddresses = new ArrayList<>();  // Script addresses (bech32)
        private List<String> policyIds = new ArrayList<>();       // Policy IDs (hex)
        private List<String> contractHashes = new ArrayList<>();  // Contract hashes (hex)
        private String sourceRegistryId;                        // Optional: external registry ID
    }
}
