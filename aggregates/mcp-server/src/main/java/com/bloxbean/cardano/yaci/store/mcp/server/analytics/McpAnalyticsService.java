package com.bloxbean.cardano.yaci.store.mcp.server.analytics;

import com.bloxbean.cardano.yaci.store.analytics.query.connection.ParquetReadConnectionProvider;
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

import java.util.LinkedHashMap;
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
 * <p>Queries run against the exported analytics data (DuckLake/Parquet, as of the last completed
 * export) through the same DuckDB engine and views as the REST query API; when live PostgreSQL
 * federation is enabled ({@code yaci.store.analytics.query.live-data-enabled=true}) the tables
 * with {@code dataScope = "historical+live"} reach the chain tip. For a real-time single-address
 * balance the dedicated {@code analytics-address-balance} tool queries PostgreSQL directly.</p>
 *
 * <p>Row limits and timeouts are those of the query layer ({@code yaci.store.analytics.query.*},
 * {@code yaci.store.analytics.duckdb.reader.query-timeout-seconds}); the effective values are
 * reported in every {@code analytics-execute-sql} result and in the {@code row_limit} query hint
 * of {@code analytics-list-tables}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.query", name = "enabled", havingValue = "true")
public class McpAnalyticsService {

    /**
     * Rows returned by {@code analytics-execute-sql} when the model does not pass {@code maxRows}:
     * enough for "top 100/500" style questions, small enough not to flood the model context.
     * Always capped by the query layer's hard limit ({@code yaci.store.analytics.query.max-rows}).
     */
    static final int DEFAULT_MAX_ROWS = 1_000;

    private final AnalyticsSchemaService schemaService;
    private final AnalyticsQueryExecutor queryExecutor;
    private final ParquetReadConnectionProvider connectionProvider;

    @Tool(name = "analytics-list-tables",
            description = "List all available Cardano analytics tables queryable via DuckDB SQL. " +
                    "Returns table names, row counts, partition strategies, date ranges, per-table 'dataScope' " +
                    "('historical' = exported data as of 'dataAsOf'; 'historical+live' = also unioned with live " +
                    "PostgreSQL up to the chain tip — only when 'liveDataActive' is true), plus global query hints " +
                    "including 'row_limit' (the row limits that apply to analytics-execute-sql). " +
                    "Use DuckDB SQL syntax for queries (PostgreSQL-compatible with extensions). " +
                    "Call this FIRST to discover what data is available, then use " +
                    "'analytics-describe-table' for column details and 'analytics-execute-sql' to query. " +
                    "KEY TABLES: transaction, block, address_utxo (largest table, FLATTENED — one row per asset, " +
                    "no JSONB), tx_input (spent outputs), epoch_stake (delegations), reward (staking rewards), " +
                    "voting_procedure (governance votes), assets (mints/burns). " +
                    "Only join tables with the same dataScope; for epoch-level live tables the current epoch " +
                    "may still change until it closes.")
    public SchemaOverview listTables() {
        return schemaService.listTables();
    }

    @Tool(name = "analytics-describe-table",
            description = "Get detailed column schema for a specific analytics table. " +
                    "Returns column names, DuckDB types, and query hints for building efficient queries. " +
                    "Data is queried via DuckDB — use DuckDB SQL syntax. " +
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
            description = "Execute a read-only DuckDB SQL query against the Cardano analytics tables listed by " +
                    "analytics-list-tables. Data is the exported analytics data (see 'dataAsOf'); tables with " +
                    "dataScope 'historical+live' also include live rows up to the chain tip. For the real-time " +
                    "balance of one address use 'analytics-address-balance' instead; for top addresses by balance " +
                    "use 'analytics-top-balances'. " +
                    "IMPORTANT QUERY GUIDELINES: " +
                    "1. Use DuckDB SQL syntax (PostgreSQL-compatible). " +
                    "2. Only SELECT/WITH statements allowed. " +
                    "3. ALWAYS add WHERE date/epoch filters on large tables (address_utxo, transaction) for partition pruning. " +
                    "4. address_utxo is FLATTENED — use asset_unit, quantity columns directly (no JSONB). " +
                    "   For ADA: WHERE asset_unit = 'lovelace'. For tokens: WHERE asset_unit = '<policyId><assetNameHex>'. " +
                    "5. Results are row-limited: at most 'maxRows' rows (default 1000, at most the server's hard limit — " +
                    "see the 'row_limit' hint of analytics-list-tables; the applied limit is returned as 'row_limit'). " +
                    "Prefer aggregation over raw rows; if 'truncated' is true, aggregate or filter instead of asking " +
                    "for more rows. " +
                    "6. Each query has a server-side timeout (returned as 'timeout_seconds'); pass 'timeoutSeconds' " +
                    "for a heavier query, up to the server maximum. On a timeout, narrow the date/epoch range first. " +
                    "7. DuckDB supports: CTEs (WITH), window functions (ROW_NUMBER, PERCENTILE_CONT), QUALIFY, GROUP BY ALL. " +
                    "8. All ADA amounts are in lovelace (1 ADA = 1,000,000 lovelace). " +
                    "9. DATE/TIMESTAMP values are returned as ISO-8601 strings (timestamps in UTC). " +
                    "Call 'analytics-list-tables' first to discover tables, 'analytics-describe-table' for column details.")
    public Map<String, Object> executeSql(
            @ToolParam(description = "DuckDB SQL query. Must be SELECT or WITH statement. " +
                    "Example: SELECT epoch, COUNT(*) as tx_count FROM transaction WHERE epoch BETWEEN 500 AND 510 GROUP BY epoch ORDER BY epoch")
            String sql,
            @ToolParam(required = false, description = "Maximum rows to return (default 1000; values above the " +
                    "server's hard limit are reduced to it).")
            Integer maxRows,
            @ToolParam(required = false, description = "Query timeout in seconds for heavy queries. Omit for the " +
                    "server default; values above the server maximum are reduced to it.")
            Integer timeoutSeconds
    ) {
        String trimmed = sql == null ? null : sql.trim();
        SqlValidator.validate(trimmed);

        // The executor caps whatever is requested at its hard limit
        Integer effectiveMaxRows = maxRows == null ? Integer.valueOf(DEFAULT_MAX_ROWS) : maxRows;

        long start = System.currentTimeMillis();
        AnalyticsQueryExecutor.QueryResult result = queryExecutor.execute(trimmed, effectiveMaxRows, timeoutSeconds);
        long elapsed = System.currentTimeMillis() - start;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("rows", result.rows());
        response.put("row_count", result.rows().size());
        response.put("row_limit", result.rowLimit());
        response.put("truncated", result.truncated());
        response.put("timeout_seconds", result.timeoutSeconds());
        response.put("execution_time_ms", elapsed);
        response.put("data_source", connectionProvider.isLiveDataActive()
                ? "DuckDB (exported analytics data, federated with live PostgreSQL for tables with dataScope 'historical+live')"
                : "DuckDB (exported analytics data)");
        return response;
    }
}
