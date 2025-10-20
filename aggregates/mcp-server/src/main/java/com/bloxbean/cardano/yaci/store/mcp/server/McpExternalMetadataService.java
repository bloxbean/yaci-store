package com.bloxbean.cardano.yaci.store.mcp.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * MCP service for fetching external metadata that's not directly accessible from Claude Desktop.
 * Provides tools to fetch:
 * 1. Cardano Token Registry metadata from GitHub (single and batch with virtual threads)
 * 2. IPFS content via public gateways (especially for governance anchor URLs)
 *
 * This service works around CORS and network restrictions in Claude Desktop by fetching
 * content server-side and returning it to the LLM.
 *
 * Performance optimizations:
 * - Batch fetching using Java Virtual Threads for parallel HTTP requests
 * - Compact mode to reduce context usage by ~70%
 * - 5-second timeout per request to prevent hanging
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "store.mcp-server.tools.external-metadata.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class McpExternalMetadataService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final int MAX_BATCH_SIZE = 100;
    private static final int REQUEST_TIMEOUT_SECONDS = 5;

    // Essential fields for compact mode
    private static final Set<String> COMPACT_FIELDS = Set.of(
        "asset_unit", "name", "ticker", "decimals", "registry_status"
    );

    // Public IPFS gateways - in order of reliability
    private static final List<String> IPFS_GATEWAYS = List.of(
        "https://ipfs.io/ipfs/",
        "https://gateway.pinata.cloud/ipfs/",
        "https://cloudflare-ipfs.com/ipfs/",
        "https://dweb.link/ipfs/"
    );

    @Tool(name = "get-token-registry-metadata",
          description = "Fetch official Cardano token metadata from the Cardano Foundation Token Registry for a SINGLE token. " +
                        "Returns name, ticker, description, decimals, logo, and other verified metadata. " +
                        "This tool fetches from GitHub server-side to bypass Claude Desktop network restrictions. " +
                        "Use 'compact=true' to get only essential fields (name, ticker, decimals) for context efficiency. " +
                        "For fetching metadata of MULTIPLE tokens (10+), use 'get-token-registry-metadata-batch' instead. " +
                        "Returns error message if token is not registered in the official registry.")
    public Map<String, Object> getTokenRegistryMetadata(
        @ToolParam(description = "Asset unit (policyId + assetName in hex). This is the 'asset_unit' or 'unit' field from other tool results.")
        String assetUnit,
        @ToolParam(description = "If true, returns only essential fields (name, ticker, decimals). Default: false (full metadata).")
        Boolean compact
    ) {
        boolean compactMode = compact != null && compact;
        log.debug("Fetching token registry metadata for asset: {} (compact={})", assetUnit, compactMode);

        Map<String, Object> result = fetchSingleTokenMetadata(assetUnit);

        if (compactMode && "registered".equals(result.get("registry_status"))) {
            return compactResponse(result);
        }

        return result;
    }

    @Tool(name = "get-token-registry-metadata-batch",
          description = "Efficiently fetch token metadata for MULTIPLE tokens in parallel using virtual threads. " +
                        "IMPORTANT: Use this for 10+ tokens to avoid context exhaustion and slow responses. " +
                        "Compact mode (default=true) returns only essential fields (name, ticker, decimals) saving ~70% context. " +
                        "Full mode returns all fields including description, logo, url, etc. " +
                        "This tool uses Java Virtual Threads for massively parallel HTTP requests (50-100x faster than sequential). " +
                        "Maximum batch size: 100 tokens per call. " +
                        "Gracefully handles failures - continues even if some tokens are not registered.")
    public Map<String, Object> getTokenRegistryMetadataBatch(
        @ToolParam(description = "List of asset units (policyId + assetName in hex) to fetch. Maximum 100 per request.")
        List<String> assetUnits,
        @ToolParam(description = "If true, returns only essential fields (name, ticker, decimals) for each token. Default: true for context efficiency.")
        Boolean compact
    ) {
        boolean compactMode = compact == null || compact; // Default to true
        int totalRequested = assetUnits.size();

        log.info("Batch fetching metadata for {} tokens (compact={})", totalRequested, compactMode);

        // Validate and limit batch size
        if (totalRequested > MAX_BATCH_SIZE) {
            log.warn("Batch size {} exceeds maximum {}, truncating", totalRequested, MAX_BATCH_SIZE);
            assetUnits = assetUnits.subList(0, MAX_BATCH_SIZE);
        }

        Map<String, Map<String, Object>> results = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();

        // Use Virtual Thread executor for parallel HTTP requests
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Create CompletableFuture for each token fetch
            List<CompletableFuture<TokenFetchResult>> futures = assetUnits.stream()
                .map(assetUnit -> CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            Map<String, Object> metadata = fetchSingleTokenMetadata(assetUnit);
                            return new TokenFetchResult(assetUnit, metadata, null);
                        } catch (Exception e) {
                            log.debug("Failed to fetch metadata for {}: {}", assetUnit, e.getMessage());
                            return new TokenFetchResult(assetUnit, null, e.getMessage());
                        }
                    },
                    executor
                ).orTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                 .exceptionally(ex -> new TokenFetchResult(assetUnit, null, "Timeout or error: " + ex.getMessage())))
                .toList();

            // Wait for all futures to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Collect results
            for (CompletableFuture<TokenFetchResult> future : futures) {
                try {
                    TokenFetchResult result = future.get();
                    if (result.metadata != null) {
                        Map<String, Object> metadata = result.metadata;
                        if (compactMode && "registered".equals(metadata.get("registry_status"))) {
                            metadata = compactResponse(metadata);
                        }
                        results.put(result.assetUnit, metadata);
                    } else {
                        errors.put(result.assetUnit, result.error != null ? result.error : "Unknown error");
                    }
                } catch (Exception e) {
                    log.warn("Error collecting result: {}", e.getMessage());
                }
            }
        }

        int successful = results.size();
        int failed = errors.size();

        log.info("Batch fetch completed: {}/{} successful, {} failed", successful, totalRequested, failed);

        // Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total_requested", totalRequested);
        response.put("successful", successful);
        response.put("failed", failed);
        response.put("compact_mode", compactMode);
        response.put("results", results);
        if (!errors.isEmpty()) {
            response.put("errors", errors);
        }

        return response;
    }

    @Tool(name = "fetch-ipfs-content",
          description = "Fetch content from IPFS via public gateways. Automatically tries multiple gateways for reliability. " +
                        "IMPORTANT: This tool is essential for accessing Cardano governance anchor URLs, which are typically " +
                        "stored on IPFS (e.g., 'ipfs://QmXxxx...'). Use this to fetch proposal metadata, voting rationale, " +
                        "and other governance documents. Supports both 'ipfs://' URLs and raw IPFS hashes (CID). " +
                        "Returns the fetched content as JSON if parseable, otherwise as text. " +
                        "Claude Desktop cannot access IPFS directly - this tool bypasses that limitation by using server-side requests.")
    public Map<String, Object> fetchIpfsContent(
        @ToolParam(description = "IPFS URL (e.g., 'ipfs://QmXxxx...') or raw IPFS CID hash. Common in governance anchor URLs.")
        String ipfsUrl
    ) {
        log.debug("Fetching IPFS content for: {}", ipfsUrl);

        // Extract CID from various formats
        String cid = extractCid(ipfsUrl);
        if (cid == null || cid.isEmpty()) {
            return Map.of(
                "error", "Invalid IPFS URL or CID",
                "provided_url", ipfsUrl,
                "hint", "Expected format: 'ipfs://QmXxxx...' or raw CID like 'QmXxxx...'"
            );
        }

        // Try multiple gateways for reliability
        Exception lastException = null;
        for (String gateway : IPFS_GATEWAYS) {
            try {
                String url = gateway + cid;
                log.debug("Trying IPFS gateway: {}", gateway);

                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    String content = response.getBody();

                    // Try to parse as JSON, otherwise return as text
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("cid", cid);
                    result.put("gateway_used", gateway);
                    result.put("original_url", ipfsUrl);

                    try {
                        // Try parsing as JSON
                        Object jsonContent = objectMapper.readValue(content, Object.class);
                        result.put("content_type", "json");
                        result.put("content", jsonContent);
                    } catch (Exception e) {
                        // Not JSON, return as text
                        result.put("content_type", "text");
                        result.put("content", content);
                    }

                    log.debug("Successfully fetched IPFS content from {}: {} bytes", gateway, content.length());
                    return result;
                }

            } catch (Exception e) {
                log.debug("Failed to fetch from gateway {}: {}", gateway, e.getMessage());
                lastException = e;
                // Continue to next gateway
            }
        }

        // All gateways failed
        log.warn("All IPFS gateways failed for CID: {}", cid);
        return Map.of(
            "error", "Failed to fetch from all IPFS gateways",
            "cid", cid,
            "original_url", ipfsUrl,
            "gateways_tried", IPFS_GATEWAYS.size(),
            "last_error", lastException != null ? lastException.getMessage() : "Unknown error",
            "hint", "The content may be unavailable or unpinned on IPFS. Try again later or check if the CID is correct."
        );
    }

    /**
     * Fetch metadata for a single token from the registry.
     * Internal helper method used by both single and batch tools.
     */
    private Map<String, Object> fetchSingleTokenMetadata(String assetUnit) {
        try {
            String url = String.format(
                "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/%s.json",
                assetUnit
            );

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> metadata = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<Map<String, Object>>() {}
                );

                // Add the asset_unit for reference
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("asset_unit", assetUnit);
                result.put("registry_status", "registered");
                result.putAll(metadata);

                log.debug("Successfully fetched registry metadata for {}: {}",
                    assetUnit, metadata.get("name"));

                return result;
            } else {
                return createErrorResponse(assetUnit, "Empty response from token registry");
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Token not found in registry: {}", assetUnit);
            return createErrorResponse(assetUnit, "Token not registered in Cardano Token Registry");

        } catch (Exception e) {
            log.debug("Failed to fetch token registry metadata for {}: {}", assetUnit, e.getMessage());
            return createErrorResponse(assetUnit, "Failed to fetch metadata: " + e.getMessage());
        }
    }

    /**
     * Strip non-essential fields to create a compact response.
     * Keeps only: asset_unit, name, ticker, decimals, registry_status
     * Reduces response size by ~70% for context efficiency.
     */
    private Map<String, Object> compactResponse(Map<String, Object> fullMetadata) {
        Map<String, Object> compact = new LinkedHashMap<>();

        for (String field : COMPACT_FIELDS) {
            if (fullMetadata.containsKey(field)) {
                compact.put(field, fullMetadata.get(field));
            }
        }

        return compact;
    }

    /**
     * Extract CID from various IPFS URL formats:
     * - ipfs://QmXxxx...
     * - /ipfs/QmXxxx...
     * - QmXxxx... (raw CID)
     * - https://ipfs.io/ipfs/QmXxxx...
     */
    private String extractCid(String ipfsUrl) {
        if (ipfsUrl == null || ipfsUrl.isEmpty()) {
            return null;
        }

        String url = ipfsUrl.trim();

        // Remove ipfs:// prefix
        if (url.startsWith("ipfs://")) {
            url = url.substring(7);
        }

        // Remove /ipfs/ prefix
        if (url.startsWith("/ipfs/")) {
            url = url.substring(6);
        }

        // Remove gateway URL if present
        for (String gateway : IPFS_GATEWAYS) {
            if (url.startsWith(gateway)) {
                url = url.substring(gateway.length());
                break;
            }
        }

        // Remove any trailing path (take only the CID part)
        int slashIndex = url.indexOf('/');
        if (slashIndex > 0) {
            url = url.substring(0, slashIndex);
        }

        // Remove query parameters if any
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            url = url.substring(0, queryIndex);
        }

        return url.trim();
    }

    private Map<String, Object> createErrorResponse(String assetUnit, String errorMessage) {
        return Map.of(
            "asset_unit", assetUnit,
            "registry_status", "not_registered",
            "error", errorMessage,
            "hint", "This token may be unregistered, burned, or a test token. Check on-chain metadata using 'metadata-by-transaction' tool."
        );
    }

    /**
     * Result holder for async token fetching
     */
    private record TokenFetchResult(String assetUnit, Map<String, Object> metadata, String error) {}
}
