package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.yaci.store.mcp.server.model.ContractTvl;
import com.bloxbean.cardano.yaci.store.mcp.server.model.ScriptFailure;
import com.bloxbean.cardano.yaci.store.mcp.server.model.ScriptInteraction;
import com.bloxbean.cardano.yaci.store.mcp.server.model.ScriptUsageStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP aggregation service for smart contract activity analytics.
 * Provides tools for monitoring contract usage, TVL, user interactions, and failures.
 * Essential for DeFi platforms, DApp developers, and contract auditing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "store.mcp-server.tools.contract-aggr.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class McpContractAggregationService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Tool(name = "script-usage-stats",
          description = "Get most used smart contracts/scripts in a time range with execution metrics. " +
                       "Returns scripts ranked by execution count, unique users, and gas consumption. " +
                       "IMPORTANT: All gas costs (unit_mem, unit_steps) are RAW execution units from the blockchain. " +
                       "Filter by scriptType: NATIVE_SCRIPT, PLUTUS_V1, PLUTUS_V2, PLUTUS_V3 (null = all types). " +
                       "Perfect for: " +
                       "- Discovering most active DeFi protocols " +
                       "- Analyzing contract adoption trends " +
                       "- Monitoring script resource consumption " +
                       "- Identifying popular DApps")
    public List<ScriptUsageStats> getScriptUsageStats(
        @ToolParam(description = "Start slot number (inclusive)")
        Long startSlot,

        @ToolParam(description = "End slot number (inclusive)")
        Long endSlot,

        @ToolParam(description = "Filter by script type: NATIVE_SCRIPT, PLUTUS_V1, PLUTUS_V2, PLUTUS_V3 (null for all)")
        String scriptType,

        @ToolParam(description = "Maximum number of results to return (default: 20, max: 100)")
        Integer limit
    ) {
        log.info("Getting script usage stats: startSlot={}, endSlot={}, scriptType={}, limit={}",
                startSlot, endSlot, scriptType, limit);

        // Validation
        if (startSlot == null || endSlot == null) {
            throw new IllegalArgumentException("startSlot and endSlot are required");
        }
        if (startSlot > endSlot) {
            throw new IllegalArgumentException("startSlot must be less than or equal to endSlot");
        }
        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 100) : 20;

        // Build SQL dynamically to handle NULL scriptType properly
        String sql;
        Map<String, Object> params = new HashMap<>();
        params.put("startSlot", startSlot);
        params.put("endSlot", endSlot);
        params.put("limit", effectiveLimit);

        if (scriptType == null || scriptType.trim().isEmpty() || scriptType.equals("null")) {
            // No script type filter
            sql = """
                SELECT
                    ts.script_hash,
                    s.script_type,
                    COUNT(*) as execution_count,
                    COUNT(DISTINCT ts.tx_hash) as unique_transactions,
                    AVG(ts.unit_mem)::bigint as avg_mem_units,
                    AVG(ts.unit_steps)::bigint as avg_cpu_units,
                    SUM(ts.unit_mem) as total_mem_units,
                    SUM(ts.unit_steps) as total_cpu_units,
                    MIN(ts.slot) as first_seen_slot,
                    MAX(ts.slot) as last_seen_slot
                FROM transaction_scripts ts
                LEFT JOIN script s ON ts.script_hash = s.script_hash
                WHERE ts.slot BETWEEN :startSlot AND :endSlot
                GROUP BY ts.script_hash, s.script_type
                ORDER BY execution_count DESC
                LIMIT :limit
                """;
        } else {
            // Filter by script type
            sql = """
                SELECT
                    ts.script_hash,
                    s.script_type,
                    COUNT(*) as execution_count,
                    COUNT(DISTINCT ts.tx_hash) as unique_transactions,
                    AVG(ts.unit_mem)::bigint as avg_mem_units,
                    AVG(ts.unit_steps)::bigint as avg_cpu_units,
                    SUM(ts.unit_mem) as total_mem_units,
                    SUM(ts.unit_steps) as total_cpu_units,
                    MIN(ts.slot) as first_seen_slot,
                    MAX(ts.slot) as last_seen_slot
                FROM transaction_scripts ts
                LEFT JOIN script s ON ts.script_hash = s.script_hash
                WHERE ts.slot BETWEEN :startSlot AND :endSlot
                  AND s.script_type = :scriptType
                GROUP BY ts.script_hash, s.script_type
                ORDER BY execution_count DESC
                LIMIT :limit
                """;
            params.put("scriptType", scriptType);
        }

        try {
            log.info("Executing script-usage-stats query with params: {}", params);
            List<ScriptUsageStats> results = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
                if (log.isDebugEnabled())
                    log.debug("Mapping row {}: script_hash={}, script_type={}, execution_count={}",
                        rowNum, rs.getString("script_hash"), rs.getString("script_type"), rs.getLong("execution_count"));
                return new ScriptUsageStats(
                    rs.getString("script_hash"),
                    rs.getString("script_type"),
                    rs.getLong("execution_count"),
                    rs.getLong("unique_transactions"),
                    rs.getLong("avg_mem_units"),
                    rs.getLong("avg_cpu_units"),
                    rs.getLong("total_mem_units"),
                    rs.getLong("total_cpu_units"),
                    rs.getLong("first_seen_slot"),
                    rs.getLong("last_seen_slot")
                );
            });
            //log.debug("Query returned {} results", results.size());
            if (results.isEmpty()) {
                log.warn("No script usage data found for slot range {}-{}", startSlot, endSlot);
            } else {
                log.info("Sample result: {}", results.get(0));
            }
            return results;
        } catch (Exception e) {
            log.error("Error executing script-usage-stats query", e);
            throw new RuntimeException("Failed to fetch script usage stats: " + e.getMessage(), e);
        }
    }

    @Tool(name = "contract-tvl-estimation",
          description = "Estimate Total Value Locked (TVL) in a smart contract by analyzing UTXOs at script addresses. " +
                       "Returns ADA and native token balances locked in the contract. " +
                       "IMPORTANT: All ADA amounts returned in BOTH lovelace AND ADA units. " +
                       "Token balances are in raw quantities (check token metadata for decimals). " +
                       "Perfect for: " +
                       "- DeFi protocol TVL tracking " +
                       "- Liquidity pool analysis " +
                       "- Escrow contract monitoring " +
                       "- Smart contract security analysis")
    public ContractTvl getContractTvl(
        @ToolParam(description = "Script hash (hex format)")
        String scriptHash
    ) {
        log.info("Getting contract TVL for script: {}", scriptHash);

        if (scriptHash == null || scriptHash.trim().isEmpty()) {
            throw new IllegalArgumentException("scriptHash is required");
        }

        String sql = """
            SELECT
                :scriptHash as script_hash,
                s.script_type,
                COUNT(*) as utxo_count,
                COALESCE(SUM(u.lovelace_amount), 0) as total_lovelace,
                u.amounts
            FROM address_utxo u
            LEFT JOIN script s ON u.reference_script_hash = s.script_hash
            WHERE u.reference_script_hash = :scriptHash
            GROUP BY s.script_type, u.amounts
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("scriptHash", scriptHash);

        // Query for ADA total and UTXO count
        String adaSql = """
            SELECT
                COUNT(*) as utxo_count,
                COALESCE(SUM(lovelace_amount), 0) as total_lovelace
            FROM address_utxo
            WHERE reference_script_hash = :scriptHash
            """;

        Map<String, Object> tvlData = jdbcTemplate.queryForMap(adaSql, params);
        Long utxoCount = ((Number) tvlData.get("utxo_count")).longValue();
        BigInteger totalLovelace = new BigInteger(tvlData.get("total_lovelace").toString());

        // Query for script type
        String scriptTypeSql = """
            SELECT script_type
            FROM script
            WHERE script_hash = :scriptHash
            """;

        String scriptType = jdbcTemplate.query(scriptTypeSql, params, rs -> {
            if (rs.next()) {
                return rs.getString("script_type");
            }
            return "UNKNOWN";
        });

        // Query for token balances (aggregate across all UTXOs)
        String tokenSql = """
            SELECT amounts
            FROM address_utxo
            WHERE reference_script_hash = :scriptHash
              AND amounts IS NOT NULL
            """;

        Map<String, BigInteger> tokenBalances = new HashMap<>();

        jdbcTemplate.query(tokenSql, params, rs -> {
            String amountsJson = rs.getString("amounts");
            if (amountsJson != null && !amountsJson.trim().isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> amounts = objectMapper.readValue(amountsJson, Map.class);
                    for (Map.Entry<String, Object> entry : amounts.entrySet()) {
                        String unit = entry.getKey();
                        Object qtyObj = entry.getValue();

                        BigInteger quantity;
                        if (qtyObj instanceof Map) {
                            // Handle nested structure like {"quantity": "1000"}
                            @SuppressWarnings("unchecked")
                            Map<String, Object> qtyMap = (Map<String, Object>) qtyObj;
                            quantity = new BigInteger(qtyMap.get("quantity").toString());
                        } else {
                            quantity = new BigInteger(qtyObj.toString());
                        }

                        tokenBalances.merge(unit, quantity, BigInteger::add);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse amounts JSON: {}", amountsJson, e);
                }
            }
        });

        return ContractTvl.create(scriptHash, scriptType, utxoCount, totalLovelace, tokenBalances);
    }

    @Tool(name = "script-address-interactions",
          description = "Get addresses interacting with a specific smart contract, ranked by interaction frequency. " +
                       "Returns list of addresses with interaction count and value locked. " +
                       "IMPORTANT: Value locked amounts returned in BOTH lovelace AND ADA units. " +
                       "Perfect for: " +
                       "- Identifying top contract users " +
                       "- Whale analysis for DeFi protocols " +
                       "- User behavior patterns " +
                       "- Smart contract adoption metrics")
    public List<ScriptInteraction> getScriptAddressInteractions(
        @ToolParam(description = "Script hash (hex format)")
        String scriptHash,

        @ToolParam(description = "Start slot number (inclusive, optional)")
        Long startSlot,

        @ToolParam(description = "End slot number (inclusive, optional)")
        Long endSlot,

        @ToolParam(description = "Maximum number of results to return (default: 20, max: 100)")
        Integer limit
    ) {
        log.info("Getting address interactions for script: {}, startSlot={}, endSlot={}, limit={}",
                scriptHash, startSlot, endSlot, limit);

        if (scriptHash == null || scriptHash.trim().isEmpty()) {
            throw new IllegalArgumentException("scriptHash is required");
        }

        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 100) : 20;

        // Build SQL with optional time range filter
        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT
                u.owner_addr as address,
                COUNT(*) as interaction_count,
                COALESCE(SUM(u.lovelace_amount), 0) as value_locked_lovelace,
                MIN(u.slot) as first_interaction_slot,
                MAX(u.slot) as last_interaction_slot
            FROM address_utxo u
            WHERE u.reference_script_hash = :scriptHash
            """);

        Map<String, Object> params = new HashMap<>();
        params.put("scriptHash", scriptHash);

        if (startSlot != null && endSlot != null) {
            if (startSlot > endSlot) {
                throw new IllegalArgumentException("startSlot must be less than or equal to endSlot");
            }
            sqlBuilder.append(" AND u.slot BETWEEN :startSlot AND :endSlot");
            params.put("startSlot", startSlot);
            params.put("endSlot", endSlot);
        }

        sqlBuilder.append("""

            GROUP BY u.owner_addr
            ORDER BY interaction_count DESC, value_locked_lovelace DESC
            LIMIT :limit
            """);
        params.put("limit", effectiveLimit);

        return jdbcTemplate.query(sqlBuilder.toString(), params, (rs, rowNum) ->
            ScriptInteraction.create(
                rs.getString("address"),
                rs.getLong("interaction_count"),
                new BigInteger(rs.getString("value_locked_lovelace")),
                rs.getLong("first_interaction_slot"),
                rs.getLong("last_interaction_slot")
            )
        );
    }

    @Tool(name = "plutus-script-failures",
          description = "Get failed Plutus script executions for debugging and monitoring. " +
                       "Returns invalid transactions with script execution details. " +
                       "Perfect for: " +
                       "- Debugging smart contract issues " +
                       "- Understanding why transactions fail " +
                       "- Monitoring contract health " +
                       "- Analyzing error patterns")
    public List<ScriptFailure> getPlutusScriptFailures(
        @ToolParam(description = "Script hash (hex format, optional - null returns all failures)")
        String scriptHash,

        @ToolParam(description = "Start slot number (inclusive)")
        Long startSlot,

        @ToolParam(description = "End slot number (inclusive)")
        Long endSlot,

        @ToolParam(description = "Maximum number of results to return (default: 20, max: 100)")
        Integer limit
    ) {
        log.info("Getting script failures: scriptHash={}, startSlot={}, endSlot={}, limit={}",
                scriptHash, startSlot, endSlot, limit);

        if (startSlot == null || endSlot == null) {
            throw new IllegalArgumentException("startSlot and endSlot are required");
        }
        if (startSlot > endSlot) {
            throw new IllegalArgumentException("startSlot must be less than or equal to endSlot");
        }

        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 100) : 20;

        String sql = """
            SELECT
                it.tx_hash,
                it.slot,
                it.block_hash,
                ts.script_hash,
                ts.purpose,
                ts.datum_hash,
                ts.redeemer_datahash as redeemer_data_hash,
                ts.unit_mem,
                ts.unit_steps
            FROM invalid_transaction it
            INNER JOIN transaction_scripts ts ON it.tx_hash = ts.tx_hash
            WHERE it.slot BETWEEN :startSlot AND :endSlot
              AND (:scriptHash IS NULL OR ts.script_hash = :scriptHash)
            ORDER BY it.slot DESC
            LIMIT :limit
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("startSlot", startSlot);
        params.put("endSlot", endSlot);
        params.put("scriptHash", scriptHash);
        params.put("limit", effectiveLimit);

        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
            new ScriptFailure(
                rs.getString("tx_hash"),
                rs.getLong("slot"),
                rs.getString("block_hash"),
                rs.getString("script_hash"),
                rs.getString("purpose"),
                rs.getString("datum_hash"),
                rs.getString("redeemer_data_hash"),
                rs.getLong("unit_mem"),
                rs.getLong("unit_steps")
            )
        );
    }
}
