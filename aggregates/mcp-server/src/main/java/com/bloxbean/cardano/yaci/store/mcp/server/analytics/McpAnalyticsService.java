package com.bloxbean.cardano.yaci.store.mcp.server.analytics;

import com.bloxbean.cardano.yaci.store.analytics.query.executor.AnalyticsQueryExecutor;
import com.bloxbean.cardano.yaci.store.analytics.query.model.SchemaOverview;
import com.bloxbean.cardano.yaci.store.analytics.query.model.TableDescription;
import com.bloxbean.cardano.yaci.store.analytics.query.service.AnalyticsSchemaService;
import com.bloxbean.cardano.yaci.store.analytics.query.service.SqlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP tools for querying Cardano analytics data via DuckDB.
 *
 * <p>Provides 3 tools for AI agent-driven analytics:
 * <ol>
 *   <li>{@code analytics-list-tables} — discover available tables</li>
 *   <li>{@code analytics-describe-table} — get column schema for a table</li>
 *   <li>{@code analytics-execute-sql} — execute DuckDB SQL queries</li>
 * </ol>
 *
 * <p>Queries run against Parquet files (T-1, 1-2 days old) via DuckDB.
 * For real-time data, dedicated tools exist: {@code analytics-address-balance}
 * (live PostgreSQL) and {@code analytics-top-balances} (Parquet full scan).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.query", name = "enabled", havingValue = "true")
public class McpAnalyticsService {

    private final AnalyticsSchemaService schemaService;
    private final AnalyticsQueryExecutor queryExecutor;

    @Tool(name = "analytics-list-tables",
            description = "List all available Cardano analytics tables queryable via DuckDB SQL. " +
                    "Data may include both historical Parquet data and live PostgreSQL data, " +
                    "unified seamlessly — check 'data_staleness_days' (0 = live data available). " +
                    "Use DuckDB SQL syntax for queries (PostgreSQL-compatible with extensions). " +
                    "Returns table names, row counts, partition strategies, date ranges, and query hints. " +
                    "Call this FIRST to discover what data is available, then use " +
                    "'analytics-describe-table' for column details and 'analytics-execute-sql' to query. " +
                    "KEY TABLES: transaction (120M rows), block (13M), address_utxo (1.47B rows, FLATTENED — " +
                    "one row per asset, no JSONB), tx_input (spent outputs), epoch_stake (delegations), " +
                    "reward (staking rewards), voting_procedure (governance votes), assets (mints/burns).")
    public SchemaOverview listTables() {
        return schemaService.listTables();
    }

    @Tool(name = "analytics-describe-table",
            description = "Get detailed column schema for a specific analytics table. " +
                    "Returns column names, DuckDB types, and query hints for building efficient queries. " +
                    "Data is in Parquet format queried via DuckDB — use DuckDB SQL syntax. " +
                    "IMPORTANT: The address_utxo table is FLATTENED — each asset per UTXO is a separate row " +
                    "with direct asset_unit, policy_id, asset_name, quantity columns (NO JSONB parsing needed). " +
                    "To find unspent UTXOs, join with tx_input: " +
                    "NOT EXISTS (SELECT 1 FROM tx_input ti WHERE ti.tx_hash = u.tx_hash AND ti.output_index = u.output_index). " +
                    "ALWAYS check the partition_column in the response — filter on it for 10-100x faster queries.")
    public TableDescription describeTable(
            @ToolParam(description = "Table name from analytics-list-tables (e.g., 'transaction', 'block', 'address_utxo', 'epoch_stake')")
            String tableName
    ) {
        return schemaService.describeTable(tableName);
    }

    @Tool(name = "analytics-execute-sql",
            description = "Execute a read-only DuckDB SQL query against Cardano analytics Parquet data. " +
                    "Data is from Parquet files (1-2 days old). For real-time address balance, " +
                    "use 'analytics-address-balance' instead. For top addresses by balance, " +
                    "use 'analytics-top-balances' instead. " +
                    "IMPORTANT QUERY GUIDELINES: " +
                    "1. Use DuckDB SQL syntax (PostgreSQL-compatible). " +
                    "2. Only SELECT/WITH statements allowed. " +
                    "3. ALWAYS add WHERE date/epoch filters on large tables (address_utxo, transaction) for partition pruning. " +
                    "4. address_utxo is FLATTENED — use asset_unit, quantity columns directly (no JSONB). " +
                    "   For ADA: WHERE asset_unit = 'lovelace'. For tokens: WHERE asset_unit = '<policyId><assetNameHex>'. " +
                    "5. Max timeout: 30 seconds. Add LIMIT for large result sets. Max 10,000 rows returned. " +
                    "6. DuckDB supports: CTEs (WITH), window functions (ROW_NUMBER, PERCENTILE_CONT), QUALIFY, GROUP BY ALL. " +
                    "7. All ADA amounts are in lovelace (1 ADA = 1,000,000 lovelace). " +
                    "Call 'analytics-list-tables' first to discover tables, 'analytics-describe-table' for column details.")
    public Map<String, Object> executeSql(
            @ToolParam(description = "DuckDB SQL query. Must be SELECT or WITH statement. " +
                    "Example: SELECT epoch, COUNT(*) as tx_count FROM transaction WHERE epoch BETWEEN 500 AND 510 GROUP BY epoch ORDER BY epoch")
            String sql
    ) {
        String trimmed = sql.trim();
        SqlValidator.validate(trimmed);

        long start = System.currentTimeMillis();
        List<Map<String, Object>> rows = queryExecutor.queryForList(trimmed);
        long elapsed = System.currentTimeMillis() - start;

        boolean truncated = AnalyticsQueryExecutor.isTruncated(rows);
        return Map.of(
                "rows", rows,
                "row_count", rows.size(),
                "execution_time_ms", elapsed,
                "truncated", truncated,
                "max_rows", AnalyticsQueryExecutor.MAX_RESULT_ROWS,
                "data_source", "DuckDB (Parquet analytics)"
        );
    }
}
