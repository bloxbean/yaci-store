package com.bloxbean.cardano.yaci.store.mcp.server.analytics;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.query.executor.AnalyticsQueryExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP tools for Cardano address balance queries.
 *
 * <p>Two tools with different data sources optimized for their use case:</p>
 * <ul>
 *   <li><b>{@code analytics-address-balance}</b> — queries <b>live PostgreSQL</b> for real-time
 *       balance of a single address. Uses {@code address_utxo_flattened} view with anti-join
 *       against {@code tx_input} via parameterized queries (no SQL injection risk).</li>
 *   <li><b>{@code analytics-top-balances}</b> — queries <b>Parquet via DuckDB</b> for top N
 *       addresses by ADA balance. Data is 1-2 days old but consistent for the full UTXO set
 *       scan (avoids the double-counting problem of unified views).</li>
 * </ul>
 *
 * <p>The unified UNION ALL views are not used for balance queries because a UTXO created
 * before the slot boundary (in Parquet) can be spent after the boundary (in live PG),
 * causing the anti-join to incorrectly report it as unspent.</p>
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.query", name = "enabled", havingValue = "true")
public class McpBalanceService {

    private final JdbcTemplate jdbcTemplate;
    private final AnalyticsQueryExecutor queryExecutor;
    private final AnalyticsStoreProperties properties;

    public McpBalanceService(JdbcTemplate jdbcTemplate,
                             AnalyticsQueryExecutor queryExecutor,
                             AnalyticsStoreProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryExecutor = queryExecutor;
        this.properties = properties;
    }

    // ========== Live PostgreSQL: Single Address Balance ==========

    /**
     * SQL for single address balance via live PostgreSQL.
     *
     * <p>Uses {@code address_utxo_flattened} (PostgreSQL view that flattens JSONB amounts)
     * with a LEFT JOIN anti-join against {@code tx_input} to find unspent outputs.
     * Parameterized with {@code ?} bind variables — no SQL injection risk.</p>
     */
    private static final String PG_ADDRESS_BALANCE_SQL = """
            SELECT
                f.asset_unit,
                f.policy_id,
                f.asset_name,
                SUM(f.quantity) as quantity,
                COUNT(*) as utxo_count
            FROM address_utxo_flattened f
            LEFT JOIN tx_input ti
                ON ti.tx_hash = f.tx_hash AND ti.output_index = f.output_index
            WHERE ti.tx_hash IS NULL
              AND (f.owner_addr = ? OR f.owner_stake_addr = ?)
            GROUP BY f.asset_unit, f.policy_id, f.asset_name
            ORDER BY
                CASE WHEN f.asset_unit = 'lovelace' THEN 0 ELSE 1 END,
                SUM(f.quantity) DESC
            LIMIT 100
            """;

    // ========== Parquet/DuckDB: Top N Address Balances ==========

    /**
     * SQL for top N address balances via DuckDB Parquet views.
     *
     * <p>Uses a CTE to deduplicate UTXOs first (some early Parquet partitions have
     * duplicate rows from re-exports), then anti-joins with deduplicated tx_input
     * to find unspent, groups by address. Data is 1-2 days old but consistent.</p>
     */
    private static final String PARQUET_TOP_BALANCES_SQL = """
            WITH dedup_utxo AS (
                SELECT DISTINCT tx_hash, output_index, owner_addr, owner_stake_addr, quantity
                FROM address_utxo
                WHERE asset_unit = 'lovelace'
            ),
            dedup_spent AS (
                SELECT DISTINCT tx_hash, output_index
                FROM tx_input
            )
            SELECT
                u.owner_addr,
                u.owner_stake_addr,
                SUM(u.quantity) / 1000000.0 as ada_balance,
                COUNT(*) as utxo_count
            FROM dedup_utxo u
            LEFT JOIN dedup_spent ti
                ON ti.tx_hash = u.tx_hash AND ti.output_index = u.output_index
            WHERE ti.tx_hash IS NULL
            GROUP BY u.owner_addr, u.owner_stake_addr
            ORDER BY ada_balance DESC
            LIMIT %d
            """;

    @Tool(name = "analytics-address-balance",
            description = "Get the REAL-TIME balance of a Cardano address from live PostgreSQL. " +
                    "Returns ADA balance and all native token balances held by the address. " +
                    "This queries the live blockchain database directly — data is current (not delayed). " +
                    "Accepts either a payment address (addr_test1... / addr1...) or a stake address (stake_test1... / stake1...). " +
                    "ADA is shown as 'lovelace' (1 ADA = 1,000,000 lovelace). " +
                    "Each row is one asset: lovelace for ADA, policyId+assetName for native tokens.")
    public Map<String, Object> getAddressBalance(
            @ToolParam(description = "Cardano address (payment address starting with 'addr...' or stake address starting with 'stake...')")
            String address
    ) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address must not be empty");
        }

        String sanitized = address.trim();
        // Bech32 addresses: alphanumeric + underscore only
        if (!sanitized.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid address format");
        }

        long start = System.currentTimeMillis();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                PG_ADDRESS_BALANCE_SQL, sanitized, sanitized);
        long elapsed = System.currentTimeMillis() - start;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("address", sanitized);
        result.put("assets", rows);
        result.put("asset_count", rows.size());
        result.put("execution_time_ms", elapsed);
        result.put("data_source", "Live PostgreSQL (real-time)");
        return result;
    }

    @Tool(name = "analytics-top-balances",
            description = "Get the top N addresses by ADA balance on Cardano. " +
                    "Scans the entire unspent UTXO set from Parquet data (1-2 days old). " +
                    "Data is NOT real-time — it reflects balances as of yesterday. " +
                    "For real-time balance of a specific address, use 'analytics-address-balance' instead. " +
                    "Returns addresses ranked by total ADA holdings with UTXO count. " +
                    "This is a heavy query that scans millions of UTXOs — may take 10-20 seconds. " +
                    "Maximum limit is 100 addresses. ADA balances are in ADA (not lovelace).")
    public Map<String, Object> getTopBalances(
            @ToolParam(description = "Number of top addresses to return (default: 10, max: 100)")
            int limit
    ) {
        if (limit <= 0) limit = 10;
        if (limit > 100) limit = 100;

        String sql = String.format(PARQUET_TOP_BALANCES_SQL, limit);

        long start = System.currentTimeMillis();
        List<Map<String, Object>> rows = queryExecutor.queryForList(sql);
        long elapsed = System.currentTimeMillis() - start;

        int bufferDays = properties.getContinuousSync().getBufferDays();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("addresses", rows);
        result.put("count", rows.size());
        result.put("execution_time_ms", elapsed);
        result.put("data_source", "Parquet (" + bufferDays + " day(s) old, full UTXO set scan)");
        return result;
    }
}
