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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP service for fetching external metadata that's not directly accessible from Claude Desktop.
 * Provides tools to fetch:
 * 1. Cardano Token Registry metadata from GitHub
 * 2. IPFS content via public gateways (especially for governance anchor URLs)
 *
 * This service works around CORS and network restrictions in Claude Desktop by fetching
 * content server-side and returning it to the LLM.
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

    // Public IPFS gateways - in order of reliability
    private static final List<String> IPFS_GATEWAYS = List.of(
        "https://ipfs.io/ipfs/",
        "https://gateway.pinata.cloud/ipfs/",
        "https://cloudflare-ipfs.com/ipfs/",
        "https://dweb.link/ipfs/"
    );

    @Tool(name = "get-token-registry-metadata",
          description = "Fetch official Cardano token metadata from the Cardano Foundation Token Registry. " +
                        "Returns name, ticker, description, decimals, logo, and other verified metadata for a token. " +
                        "This tool fetches from GitHub server-side to bypass Claude Desktop network restrictions. " +
                        "Use this whenever you have an 'asset_unit' or 'unit' field from other tools to display " +
                        "human-readable token information like 'TokenName (TICKER)' instead of hex values. " +
                        "Returns error message if token is not registered in the official registry.")
    public Map<String, Object> getTokenRegistryMetadata(
        @ToolParam(description = "Asset unit (policyId + assetName in hex). This is the 'asset_unit' or 'unit' field from other tool results.")
        String assetUnit
    ) {
        log.debug("Fetching token registry metadata for asset: {}", assetUnit);

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
            log.warn("Failed to fetch token registry metadata for {}: {}", assetUnit, e.getMessage());
            return createErrorResponse(assetUnit, "Failed to fetch metadata: " + e.getMessage());
        }
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
}
