package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.yaci.store.mcp.server.model.ContractTvl;
import com.bloxbean.cardano.yaci.store.mcp.server.model.ContractTvlSnapshot;
import com.bloxbean.cardano.yaci.store.mcp.server.model.ScriptFailure;
import com.bloxbean.cardano.yaci.store.mcp.server.model.ScriptInteraction;
import com.bloxbean.cardano.yaci.store.mcp.server.model.ScriptUsageStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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

    @Value("${store.account.address-balance-enabled:false}")
    private boolean balanceTablesEnabled;

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
          description = "Estimate Total Value Locked (TVL) in a smart contract. " +
                       "Supports payment credential search to handle modern DApps with Franken addresses " +
                       "(script payment + user stake combinations). " +
                       "Returns current ADA and native token balances locked across ALL addresses " +
                       "controlled by the script. " +
                       "ULTRA-FAST when balance tables enabled (10-50ms vs 50-200ms fallback). " +
                       "IMPORTANT: Returns amounts in BOTH lovelace AND ADA units. " +
                       "Perfect for: DeFi protocol TVL tracking, liquidity pool analysis, escrow monitoring, " +
                       "smart contract security analysis.")
    public ContractTvl getContractTvl(
        @ToolParam(description = "Script hash (56-char hex payment credential)")
        String scriptHash,

        @ToolParam(description = "Search by payment credential (aggregates across all Franken addresses). " +
                                 "Default: true. Set false to query only canonical enterprise address.")
        Boolean searchByPaymentCredential
    ) {
        if (scriptHash == null || scriptHash.trim().isEmpty()) {
            throw new IllegalArgumentException("scriptHash is required");
        }

        // Determine search mode
        boolean usePaymentCredSearch = searchByPaymentCredential != null
            ? searchByPaymentCredential
            : true;  // Default: aggregate by payment credential

        log.info("[CONTRACT TVL] Script: {}, PaymentCredSearch: {}, BalanceTablesEnabled: {}",
                scriptHash, usePaymentCredSearch, balanceTablesEnabled);

        // Check if balance tables available
        if (balanceTablesEnabled && usePaymentCredSearch) {
            return getContractTvlOptimized(scriptHash);
        } else {
            // Fallback to UTXO mode
            return getContractTvlFromUtxo(scriptHash);
        }
    }

    /**
     * Optimized TVL query using balance tables + materialized view JOIN.
     * Handles 10,000+ Franken addresses efficiently (20-100ms).
     */
    private ContractTvl getContractTvlOptimized(String scriptHash) {
        log.info("[CONTRACT TVL OPTIMIZED] Payment credential search for: {}", scriptHash);

        // Single JOIN query - handles 10,000+ addresses efficiently
        String sql = """
            SELECT
                abc.unit,
                SUM(abc.quantity) as total_quantity,
                COUNT(DISTINCT abc.address) as address_count
            FROM address_balance_current abc
            INNER JOIN address_credential_mapping acm
                ON abc.address = acm.address
            WHERE acm.payment_credential = :scriptHash
              AND abc.quantity > 0
            GROUP BY abc.unit
            """;

        Map<String, BigInteger> balances = new HashMap<>();
        AtomicLong addressCount = new AtomicLong(0);

        jdbcTemplate.query(sql, Map.of("scriptHash", scriptHash), rs -> {
            String unit = rs.getString("unit");
            BigInteger quantity = rs.getBigDecimal("total_quantity").toBigInteger();
            balances.put(unit, quantity);

            // Track address count from first row
            if (addressCount.get() == 0) {
                addressCount.set(rs.getLong("address_count"));
            }
        });

        // Extract lovelace and tokens
        BigInteger totalLovelace = balances.getOrDefault("lovelace", BigInteger.ZERO);
        Map<String, BigInteger> tokenBalances = new HashMap<>(balances);
        tokenBalances.remove("lovelace");

        log.info("[CONTRACT TVL OPTIMIZED] Found {} addresses, {} ADA, {} tokens",
                addressCount.get(), totalLovelace.divide(BigInteger.valueOf(1000000)), tokenBalances.size());

        return ContractTvl.create(
            scriptHash,
            "SCRIPT",
            addressCount.get(),
            totalLovelace,
            tokenBalances
        );
    }

    /**
     * Fallback TVL query using address_utxo table.
     * Queries unspent UTXOs by payment credential (50-200ms).
     */
    private ContractTvl getContractTvlFromUtxo(String scriptHash) {
        log.info("[CONTRACT TVL FALLBACK] Using address_utxo for: {}", scriptHash);

        // Query unspent UTXOs by payment credential (FIXED LOGIC)
        // CRITICAL: Join with tx_input to filter out spent UTXOs
        String sql = """
            SELECT
                au.lovelace_amount,
                au.amounts
            FROM address_utxo au
            LEFT JOIN tx_input ti
                ON ti.tx_hash = au.tx_hash
                AND ti.output_index = au.output_index
            WHERE au.owner_payment_credential = :scriptHash
              AND ti.tx_hash IS NULL
            """;

        BigInteger totalLovelace = BigInteger.ZERO;
        Map<String, BigInteger> tokenBalances = new HashMap<>();
        AtomicLong utxoCount = new AtomicLong(0);

        // Need to use array to allow mutation in lambda
        final BigInteger[] lovelaceSum = {BigInteger.ZERO};

        jdbcTemplate.query(sql, Map.of("scriptHash", scriptHash), rs -> {
            utxoCount.incrementAndGet();

            // Aggregate lovelace
            long lovelace = rs.getLong("lovelace_amount");
            lovelaceSum[0] = lovelaceSum[0].add(BigInteger.valueOf(lovelace));

            // Parse JSONB for tokens
            String amountsJson = rs.getString("amounts");
            if (amountsJson != null && !amountsJson.trim().isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> amounts = objectMapper.readValue(amountsJson, Map.class);
                    for (Map.Entry<String, Object> entry : amounts.entrySet()) {
                        String unit = entry.getKey();
                        BigInteger quantity = new BigInteger(entry.getValue().toString());
                        tokenBalances.merge(unit, quantity, BigInteger::add);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse amounts JSON: {}", amountsJson, e);
                }
            }
        });

        totalLovelace = lovelaceSum[0];

        log.info("[CONTRACT TVL FALLBACK] Found {} UTXOs, {} ADA, {} tokens",
                utxoCount.get(), totalLovelace.divide(BigInteger.valueOf(1000000)), tokenBalances.size());

        return ContractTvl.create(
            scriptHash,
            "SCRIPT",
            utxoCount.get(),
            totalLovelace,
            tokenBalances
        );
    }

    @Tool(name = "contract-tvl-timeline",
          description = "Track how contract TVL evolved over time across an epoch range. " +
                       "Returns TVL snapshots for each epoch showing ADA and token balances. " +
                       "Perfect for: TVL growth analysis, liquidity trends, protocol adoption metrics. " +
                       "ULTRA-FAST: Uses pre-aggregated balance tables. " +
                       "⚠️ ONLY AVAILABLE when balance tables are enabled (store.account.address-balance-enabled=true)")
    @ConditionalOnProperty(
        name = "store.account.address-balance-enabled",
        havingValue = "true"
    )
    public List<ContractTvlSnapshot> getContractTvlTimeline(
        @ToolParam(description = "Script hash (56-char hex payment credential)")
        String scriptHash,

        @ToolParam(description = "Start epoch (inclusive)")
        Integer startEpoch,

        @ToolParam(description = "End epoch (inclusive)")
        Integer endEpoch,

        @ToolParam(description = "Search by payment credential (aggregates across Franken addresses). Default: true")
        Boolean searchByPaymentCredential
    ) {
        if (scriptHash == null || scriptHash.trim().isEmpty()) {
            throw new IllegalArgumentException("scriptHash is required");
        }
        if (startEpoch == null || endEpoch == null) {
            throw new IllegalArgumentException("startEpoch and endEpoch are required");
        }
        if (startEpoch > endEpoch) {
            throw new IllegalArgumentException("startEpoch must be <= endEpoch");
        }

        boolean usePaymentCredSearch = searchByPaymentCredential != null
            ? searchByPaymentCredential
            : true;

        log.info("[CONTRACT TVL TIMELINE] Script: {}, Epochs: {}-{}, PaymentCred: {}",
                 scriptHash, startEpoch, endEpoch, usePaymentCredSearch);

        // JOIN with credential mapping for Franken addresses
        // CRITICAL: For each epoch, get most recent balance AT OR BEFORE that epoch
        String sql = """
            WITH epoch_series AS (
                SELECT generate_series(:startEpoch, :endEpoch) as epoch
            ),
            latest_balances_per_epoch AS (
                SELECT DISTINCT ON (es.epoch, ab.address, ab.unit)
                    es.epoch as target_epoch,
                    ab.address,
                    ab.unit,
                    ab.quantity
                FROM epoch_series es
                CROSS JOIN address_credential_mapping acm
                LEFT JOIN address_balance ab
                    ON ab.address = acm.address
                    AND ab.epoch <= es.epoch
                WHERE acm.payment_credential = :scriptHash
                ORDER BY es.epoch, ab.address, ab.unit, ab.epoch DESC NULLS LAST, ab.slot DESC NULLS LAST
            )
            SELECT
                target_epoch as epoch,
                unit,
                SUM(quantity) FILTER (WHERE quantity > 0) as total_quantity,
                COUNT(DISTINCT address) FILTER (WHERE quantity > 0) as address_count
            FROM latest_balances_per_epoch
            WHERE unit IS NOT NULL
            GROUP BY target_epoch, unit
            ORDER BY target_epoch, unit
            """;

        Map<String, Object> params = Map.of(
            "scriptHash", scriptHash,
            "startEpoch", startEpoch,
            "endEpoch", endEpoch
        );

        // Group results by epoch
        Map<Integer, Map<String, BigInteger>> epochBalances = new HashMap<>();
        Map<Integer, Long> epochAddressCounts = new HashMap<>();

        jdbcTemplate.query(sql, params, rs -> {
            int epoch = rs.getInt("epoch");
            String unit = rs.getString("unit");

            // Handle NULL from SUM aggregate (happens when no positive balances for epoch/unit)
            BigDecimal quantityDecimal = rs.getBigDecimal("total_quantity");
            BigInteger quantity = quantityDecimal != null ? quantityDecimal.toBigInteger() : BigInteger.ZERO;
            long addressCount = rs.getLong("address_count");

            epochBalances.computeIfAbsent(epoch, k -> new HashMap<>()).put(unit, quantity);
            epochAddressCounts.put(epoch, addressCount);
        });

        // Convert to snapshot list
        List<ContractTvlSnapshot> snapshots = epochBalances.entrySet().stream()
            .map(entry -> {
                int epoch = entry.getKey();
                Map<String, BigInteger> balances = entry.getValue();

                BigInteger lovelace = balances.getOrDefault("lovelace", BigInteger.ZERO);
                Map<String, BigInteger> tokens = new HashMap<>(balances);
                tokens.remove("lovelace");

                return new ContractTvlSnapshot(
                    epoch,
                    scriptHash,
                    epochAddressCounts.get(epoch),
                    lovelace,
                    tokens
                );
            })
            .sorted(Comparator.comparingInt(ContractTvlSnapshot::epoch))
            .collect(Collectors.toList());

        log.info("[CONTRACT TVL TIMELINE] Returned {} epoch snapshots", snapshots.size());

        return snapshots;
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
