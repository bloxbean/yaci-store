package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.mcp.server.model.TransactionMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP service for querying transaction metadata.
 * Provides access to on-chain metadata including NFT metadata (label 721),
 * fungible token metadata (label 20), and custom metadata.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = {"store.metadata.enabled", "store.mcp-server.tools.metadata.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpMetadataService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Tool(name = "metadata-by-transaction",
          description = "Get all metadata for a specific transaction. " +
                       "Returns all metadata labels attached to the transaction with both CBOR and JSON representations. " +
                       "Useful for inspecting NFT metadata, token metadata, or custom data attached to transactions.")
    public List<TransactionMetadata> getMetadataByTransaction(
        @ToolParam(description = "Transaction hash") String txHash
    ) {
        log.debug("Getting metadata for transaction: {}", txHash);

        String sql = """
            SELECT id, slot, tx_hash, label, body, cbor, block, block_time
            FROM transaction_metadata
            WHERE tx_hash = :txHash
            ORDER BY label
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("txHash", txHash);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new TransactionMetadata(
                rs.getString("id"),
                rs.getLong("slot"),
                rs.getString("tx_hash"),
                rs.getString("label"),
                rs.getString("body"),
                rs.getString("cbor"),
                rs.getLong("block"),
                rs.getLong("block_time")
            )
        );
    }

    @Tool(name = "metadata-by-label",
          description = "Find metadata by specific label across all transactions. " +
                       "Common labels: 721 (NFT metadata), 20 (fungible token metadata), 674 (governance). " +
                       "Returns paginated results ordered by most recent first. " +
                       "Useful for discovering NFTs, tokens, or analyzing specific metadata types.")
    public List<TransactionMetadata> getMetadataByLabel(
        @ToolParam(description = "Metadata label (e.g., '721' for NFTs, '20' for FTs)") String label,
        @ToolParam(description = "Page number (0-based, default: 0)") Integer page,
        @ToolParam(description = "Results per page (default: 10, max: 200)") Integer count
    ) {
        log.debug("Getting metadata for label: {}, page: {}, count: {}", label, page, count);

        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveCount = (count != null && count > 0) ? Math.min(count, 200) : 10;
        int offset = effectivePage * effectiveCount;

        String sql = """
            SELECT id, slot, tx_hash, label, body, cbor, block, block_time
            FROM transaction_metadata
            WHERE label = :label
            ORDER BY slot DESC
            LIMIT :limit OFFSET :offset
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("label", label);
        params.put("limit", effectiveCount);
        params.put("offset", offset);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new TransactionMetadata(
                rs.getString("id"),
                rs.getLong("slot"),
                rs.getString("tx_hash"),
                rs.getString("label"),
                rs.getString("body"),
                rs.getString("cbor"),
                rs.getLong("block"),
                rs.getLong("block_time")
            )
        );
    }

    @Tool(name = "metadata-by-transaction-and-label",
          description = "Get specific metadata label for a transaction. " +
                       "Returns the metadata entry for a specific label within a transaction. " +
                       "Useful when you know both the transaction and the specific metadata label you're interested in.")
    public TransactionMetadata getMetadataByTransactionAndLabel(
        @ToolParam(description = "Transaction hash") String txHash,
        @ToolParam(description = "Metadata label") String label
    ) {
        log.debug("Getting metadata for transaction: {}, label: {}", txHash, label);

        String sql = """
            SELECT id, slot, tx_hash, label, body, cbor, block, block_time
            FROM transaction_metadata
            WHERE tx_hash = :txHash
              AND label = :label
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("txHash", txHash);
        params.put("label", label);

        List<TransactionMetadata> results = jdbcTemplate.query(sql, params,
            (rs, rowNum) -> new TransactionMetadata(
                rs.getString("id"),
                rs.getLong("slot"),
                rs.getString("tx_hash"),
                rs.getString("label"),
                rs.getString("body"),
                rs.getString("cbor"),
                rs.getLong("block"),
                rs.getLong("block_time")
            )
        );

        if (results.isEmpty()) {
            throw new RuntimeException("Metadata not found for transaction: " + txHash + " and label: " + label);
        }

        return results.get(0);
    }
}
