package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.plutus.spec.serializers.PlutusDataJsonConverter;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.mcp.server.model.DatumDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * MCP service for querying Plutus datums.
 * Provides access to on-chain datums with both CBOR and JSON representations.
 * Essential for smart contract data inspection and analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = {"store.script.enabled", "store.mcp-server.tools.datum.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpDatumService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Tool(name = "datum-by-hash",
          description = "Get datum by its hash with both CBOR and JSON representations. " +
                       "Returns the Plutus datum data in both raw CBOR format and converted JSON format. " +
                       "Essential for understanding smart contract state and parameters. " +
                       "JSON conversion uses PlutusData deserialization.")
    public DatumDetails getDatumByHash(
        @ToolParam(description = "Datum hash (blake2b-256)") String datumHash
    ) {
        log.debug("Getting datum for hash: {}", datumHash);

        String sql = """
            SELECT hash, datum, created_at_tx
            FROM datum
            WHERE hash = :hash
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("hash", datumHash);

        List<DatumDetails> results = jdbcTemplate.query(sql, params,
            (rs, rowNum) -> {
                String hash = rs.getString("hash");
                String datumCbor = rs.getString("datum");
                String createdAtTx = rs.getString("created_at_tx");

                // Convert CBOR to JSON
                String datumJson;
                try {
                    PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(datumCbor));
                    datumJson = PlutusDataJsonConverter.toJson(plutusData);
                } catch (Exception e) {
                    log.warn("Failed to convert datum to JSON: {}", datumHash, e);
                    datumJson = "{\"error\": \"Failed to deserialize datum: " + e.getMessage() + "\"}";
                }

                return new DatumDetails(hash, datumCbor, datumJson, createdAtTx);
            }
        );

        if (results.isEmpty()) {
            throw new RuntimeException("Datum not found with hash: " + datumHash);
        }

        return results.get(0);
    }

    @Tool(name = "datums-by-transaction",
          description = "Get all datums used in a specific transaction. " +
                       "Returns all Plutus datums associated with the transaction's script executions. " +
                       "Useful for analyzing smart contract interactions and understanding transaction data flow. " +
                       "Joins transaction_scripts with datum table to find all datums.")
    public List<DatumDetails> getDatumsByTransaction(
        @ToolParam(description = "Transaction hash") String txHash
    ) {
        log.debug("Getting datums for transaction: {}", txHash);

        String sql = """
            SELECT DISTINCT d.hash, d.datum, d.created_at_tx
            FROM datum d
            JOIN transaction_scripts ts ON d.hash = ts.datum_hash
            WHERE ts.tx_hash = :txHash
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("txHash", txHash);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> {
                String hash = rs.getString("hash");
                String datumCbor = rs.getString("datum");
                String createdAtTx = rs.getString("created_at_tx");

                // Convert CBOR to JSON
                String datumJson;
                try {
                    PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(datumCbor));
                    datumJson = PlutusDataJsonConverter.toJson(plutusData);
                } catch (Exception e) {
                    log.warn("Failed to convert datum to JSON: {}", hash, e);
                    datumJson = "{\"error\": \"Failed to deserialize datum: " + e.getMessage() + "\"}";
                }

                return new DatumDetails(hash, datumCbor, datumJson, createdAtTx);
            }
        );
    }

    @Tool(name = "recent-datums",
          description = "Get recently created datums ordered by creation time. " +
                       "Returns paginated list of most recent datums with both CBOR and JSON. " +
                       "Useful for exploring new smart contract deployments and discovering recent contract activity. " +
                       "Page is 0-based, default 50 results per page.")
    public List<DatumDetails> getRecentDatums(
        @ToolParam(description = "Page number (0-based, default: 0)") Integer page,
        @ToolParam(description = "Results per page (default: 50, max: 100)") Integer count
    ) {
        log.debug("Getting recent datums: page={}, count={}", page, count);

        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveCount = (count != null && count > 0) ? Math.min(count, 100) : 50;
        int offset = effectivePage * effectiveCount;

        String sql = """
            SELECT hash, datum, created_at_tx
            FROM datum
            ORDER BY create_datetime DESC
            LIMIT :limit OFFSET :offset
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("limit", effectiveCount);
        params.put("offset", offset);

        return jdbcTemplate.query(sql, params,
            (rs, rowNum) -> {
                String hash = rs.getString("hash");
                String datumCbor = rs.getString("datum");
                String createdAtTx = rs.getString("created_at_tx");

                // Convert CBOR to JSON
                String datumJson;
                try {
                    PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(datumCbor));
                    datumJson = PlutusDataJsonConverter.toJson(plutusData);
                } catch (Exception e) {
                    log.warn("Failed to convert datum to JSON: {}", hash, e);
                    datumJson = "{\"error\": \"Failed to deserialize datum: " + e.getMessage() + "\"}";
                }

                return new DatumDetails(hash, datumCbor, datumJson, createdAtTx);
            }
        );
    }
}
