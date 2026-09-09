package com.bloxbean.cardano.yaci.store.analytics.query.service;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.query.connection.ParquetReadConnectionProvider;
import com.bloxbean.cardano.yaci.store.analytics.query.connection.ParquetTableRegistry;
import com.bloxbean.cardano.yaci.store.analytics.query.executor.AnalyticsQueryExecutor;
import com.bloxbean.cardano.yaci.store.analytics.query.model.*;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schema discovery service for analytics Parquet tables.
 *
 * <p>Provides table listings, column schemas, and metadata that AI agents use
 * to dynamically build DuckDB SQL queries. Caches row counts and date ranges
 * to avoid repeated scans.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.query", name = "enabled", havingValue = "true")
public class AnalyticsSchemaService {

    private final ParquetTableRegistry tableRegistry;
    private final ParquetReadConnectionProvider connectionProvider;
    private final AnalyticsStoreProperties properties;
    private final AnalyticsQueryExecutor queryExecutor;
    private final ObjectProvider<ExportStateService> exportStateServiceProvider;

    // Cached per table: row count and date range (expensive to compute on large tables)
    private final Map<String, Long> rowCountCache = new ConcurrentHashMap<>();
    private final Map<String, TableInfo.DateRange> dateRangeCache = new ConcurrentHashMap<>();
    private final Map<String, List<ColumnSchema>> columnCache = new ConcurrentHashMap<>();
    private final Set<String> columnFailures = ConcurrentHashMap.newKeySet();
    private final Set<String> dateRangeFailures = ConcurrentHashMap.newKeySet();

    @PostConstruct
    void warmCache() {
        log.info("Warming schema cache for {} tables...", tableRegistry.getTableNames().size());
        for (String table : tableRegistry.getTableNames()) {
            try {
                getColumns(table);
            } catch (Exception e) {
                log.warn("Failed to cache schema for table '{}' ({})",
                        table, e.getClass().getSimpleName());
            }
        }
        log.info("Schema cache warmed for {} tables", columnCache.size());
    }

    public SchemaOverview listTables() {
        int bufferDays = properties.getContinuousSync().getBufferDays();

        List<TableInfo> tables = new ArrayList<>();
        for (String tableName : tableRegistry.getTableNames()) {
            TableMetadata meta = TableMetadata.forTable(tableName);
            if (meta == null) meta = TableMetadata.defaultFor(tableName);

            long rowCount = getRowCount(tableName);
            TableInfo.DateRange dateRange = getDateRange(tableName, meta.partitionColumn());

            tables.add(new TableInfo(
                    tableName,
                    meta.description(),
                    rowCount,
                    meta.partitionStrategy(),
                    meta.partitionColumn(),
                    dateRange,
                    dataScope(tableName)
            ));
        }

        // Sort: largest tables first (most useful for analytics)
        tables.sort((a, b) -> Long.compare(b.rowCount(), a.rowCount()));
        String dataAsOf = resolveDataAsOf(tables);

        Map<String, String> queryHints = new LinkedHashMap<>();
        queryHints.put("partition_pruning",
                "Always include WHERE date = '...' or epoch BETWEEN ... for performance. DuckDB prunes Hive partitions automatically.");
        queryHints.put("optional_tables",
                "address, address_balance, address_tx_amount, epoch, and transaction_witness depend on optional source features. " +
                "If absent, the table is unavailable. If listed with rowCount=0, it is currently empty; that alone does not prove " +
                "the feature is disabled. Use GET /api/v1/analytics/query/schema/{tableName} or, when MCP is enabled, " +
                "analytics-describe-table for table-specific fallback guidance.");
        queryHints.put("unspent_utxos",
                "For a current single-address balance without MCP, use GET /addresses/{address}/amounts. " +
                "When MCP is enabled, use 'analytics-address-balance'; for top addresses by balance use " +
                "'analytics-top-balances' (exported data, " + properties.getContinuousSync().getBufferDays() + " day(s) old).");
        queryHints.put("large_tables",
                "address_utxo (1.47B rows) and transaction (120M rows) benefit most from partition filters.");
        queryHints.put("sql_dialect",
                "Use DuckDB SQL. Most PostgreSQL syntax works. PERCENTILE_CONT, CTEs, window functions, QUALIFY all supported. Use list_value() instead of ARRAY[].");
        queryHints.put("lovelace",
                "All ADA amounts are in lovelace (1 ADA = 1,000,000 lovelace). Divide by 1000000.0 for ADA.");
        queryHints.put("row_limit",
                "Ad-hoc SQL returns at most " + queryExecutor.getDefaultRowLimit() + " rows unless the request sets "
                + "'maxRows' (hard limit " + queryExecutor.getHardRowLimit() + "); a bare 'SELECT * FROM t' is "
                + "cut at that limit. Aggregate or filter instead of paging through raw rows. Over REST, truncation "
                + "is signalled by the response headers X-Analytics-Row-Limit and X-Analytics-Truncated (true = "
                + "more rows existed); MCP tool results carry a 'truncated' field.");

        String engine = "DuckDB (in-memory, reading the exported analytics data files)";

        boolean live = connectionProvider.isLiveDataActive();
        long liveTables = tables.stream().filter(t -> "historical+live".equals(t.dataScope())).count();
        String note;
        if (live) {
            note = "Live PostgreSQL federation is active: " + liveTables + " of " + tables.size()
                    + " tables (dataScope 'historical+live') union the exported data with live rows and reach the "
                    + "current chain tip; the remaining tables (dataScope 'historical') contain exported data as of "
                    + dataAsOf + " (configured finality buffer: " + bufferDays + " day(s)). For epoch-level tables the current epoch's rows "
                    + "come live from PostgreSQL and may still change until the epoch closes. "
                    + "For a current address balance use GET /addresses/{address}/amounts or, when MCP is enabled, "
                    + "'analytics-address-balance'.";
        } else {
            note = "Historical analytics data as of " + dataAsOf +
                    " (configured finality buffer: " + bufferDays + " day(s)). " +
                    "For a current address balance use GET /addresses/{address}/amounts or, when MCP is enabled, " +
                    "'analytics-address-balance'.";
        }

        return new SchemaOverview(
                engine,
                "DuckDB SQL (PostgreSQL-compatible with extensions)",
                bufferDays,
                dataAsOf,
                live,
                note,
                tables,
                queryHints
        );
    }

    /** Refresh successful schema metadata every 30 minutes. */
    @Scheduled(fixedDelay = 1_800_000, initialDelay = 1_800_000)
    void invalidateMetadataCaches() {
        rowCountCache.clear();
        dateRangeCache.clear();
        columnCache.clear();
    }

    /** Retry failed metadata scans after five minutes without evicting successful results. */
    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
    void clearMetadataFailures() {
        rowCountCache.entrySet().removeIf(entry -> entry.getValue() == -1L);
        columnFailures.clear();
        dateRangeFailures.clear();
    }

    private String resolveDataAsOf(List<TableInfo> tables) {
        ExportStateService stateService = exportStateServiceProvider.getIfAvailable();
        if (stateService == null) return "unknown";

        LocalDate commonEnd = null;
        for (TableInfo table : tables) {
            if (!"DAILY".equals(table.partitionStrategy())) continue;
            try {
                LocalDate tableEnd = stateService.getCompletedPartitions(table.name()).stream()
                        .filter(partition -> partition.startsWith("date="))
                        .map(partition -> partition.substring("date=".length()))
                        .map(AnalyticsSchemaService::parseDate)
                        .filter(Objects::nonNull)
                        .max(LocalDate::compareTo)
                        .orElse(null);
                if (tableEnd != null && (commonEnd == null || tableEnd.isBefore(commonEnd))) {
                    commonEnd = tableEnd;
                }
            } catch (Exception e) {
                log.warn("Failed to resolve data freshness for '{}' ({})",
                        table.name(), e.getClass().getSimpleName());
            }
        }
        return commonEnd == null ? "unknown" : commonEnd.toString();
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String dataScope(String tableName) {
        return connectionProvider.isFederated(tableName) ? "historical+live" : "historical";
    }

    public TableDescription describeTable(String tableName) {
        if (!tableRegistry.getTableNames().contains(tableName)) {
            throw new IllegalArgumentException("Unknown table: " + tableName + ". Use analytics-list-tables to see available tables.");
        }

        TableMetadata meta = TableMetadata.forTable(tableName);
        if (meta == null) meta = TableMetadata.defaultFor(tableName);

        List<ColumnSchema> columns = getColumns(tableName);
        long rowCount = getRowCount(tableName);

        String engine = "DuckDB";

        return new TableDescription(
                tableName,
                engine,
                meta.description(),
                rowCount,
                meta.partitionStrategy(),
                meta.partitionColumn(),
                dataScope(tableName),
                columns,
                meta.queryHints()
        );
    }

    private List<ColumnSchema> getColumns(String tableName) {
        List<ColumnSchema> cached = columnCache.get(tableName);
        if (cached != null) {
            return cached;
        }
        if (columnFailures.contains(tableName)) {
            throw new SchemaUnavailableException(
                    "Schema for table '" + tableName + "' is temporarily unavailable");
        }

        List<ColumnSchema> columns = new ArrayList<>();
        try (Connection conn = connectionProvider.getReadConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE \"" + tableName.replace("\"", "\"\"") + "\"")) {
            while (rs.next()) {
                columns.add(new ColumnSchema(rs.getString("column_name"), rs.getString("column_type")));
            }
            List<ColumnSchema> immutableColumns = List.copyOf(columns);
            List<ColumnSchema> existing = columnCache.putIfAbsent(tableName, immutableColumns);
            return existing != null ? existing : immutableColumns;
        } catch (Exception e) {
            log.error("Failed to describe table '{}' ({})",
                    tableName, e.getClass().getSimpleName());
            // Retry after the scheduled five-minute failure-marker cleanup.
            columnFailures.add(tableName);
            throw new SchemaUnavailableException(
                    "Schema for table '" + tableName + "' is temporarily unavailable");
        }
    }

    public static class SchemaUnavailableException extends RuntimeException {
        public SchemaUnavailableException(String message) {
            super(message);
        }
    }

    private long getRowCount(String tableName) {
        Long cached = rowCountCache.get(tableName);
        if (cached != null) {
            return cached;
        }

        try (Connection conn = connectionProvider.getReadConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(connectionProvider.getQueryTimeoutSeconds());
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM \"" + tableName.replace("\"", "\"\"") + "\"")) {
                long rowCount = rs.next() ? rs.getLong(1) : 0L;
                Long existing = rowCountCache.putIfAbsent(tableName, rowCount);
                return existing != null ? existing : rowCount;
            }
        } catch (Exception e) {
            log.warn("Failed to count rows for '{}' ({})",
                    tableName, e.getClass().getSimpleName());
            // Cache the unavailable marker until the scheduled five-minute failure retry so
            // repeated /schema calls do not rerun the same expensive timed-out scan.
            rowCountCache.putIfAbsent(tableName, -1L);
            return -1L;
        }
    }

    private TableInfo.DateRange getDateRange(String tableName, String partitionColumn) {
        if (partitionColumn == null) return null;
        if (dateRangeFailures.contains(tableName)) return null;

        return dateRangeCache.computeIfAbsent(tableName, t -> {
            String col = "date".equals(partitionColumn) ? "date" : partitionColumn;
            try (Connection conn = connectionProvider.getReadConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(connectionProvider.getQueryTimeoutSeconds());
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT MIN(\"" + col + "\") as min_val, MAX(\"" + col + "\") as max_val FROM \"" + t.replace("\"", "\"\"") + "\"")) {
                    if (rs.next()) {
                        String min = rs.getString("min_val");
                        String max = rs.getString("max_val");
                        return new TableInfo.DateRange(min, max);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get date range for '{}' ({})",
                        t, e.getClass().getSimpleName());
                dateRangeFailures.add(t);
            }
            return null;
        });
    }
}
