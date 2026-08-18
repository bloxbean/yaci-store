package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.ducklake.DuckLakeCatalogSnapshotReader;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DuckDB connection provider that creates views over the exported Parquet data and optionally
 * federates live PostgreSQL data via {@code postgres_scanner}.
 *
 * <p>On startup, creates a DuckDB view for each table discovered by {@link ParquetTableRegistry}:</p>
 * <ul>
 *   <li><b>Direct Parquet storage</b>: {@code read_parquet('{export}/{table}/**{@literal /}*.parquet')}
 *       with hive partitioning.</li>
 *   <li><b>DuckLake storage</b>: {@code read_parquet([...committed data files...])} where the file
 *       list is the current committed catalog snapshot obtained through
 *       {@link DuckLakeCatalogSnapshotReader}. The catalog itself is <em>never</em> attached to this
 *       DuckDB instance: DuckDB does not allow two in-process instances to open the same catalog
 *       file, and the export writer already holds it. Reading only committed files also keeps
 *       in-flight or aborted export files out of the views.</li>
 * </ul>
 *
 * <p>When {@code yaci.store.analytics.query.live-data-enabled=true}, the provider also:</p>
 * <ol>
 *   <li>Installs the {@code postgres_scanner} extension</li>
 *   <li>Attaches the source PostgreSQL database as {@code pg_live}</li>
 *   <li>Creates unified views that UNION ALL Parquet (historical) with PostgreSQL (live)</li>
 *   <li>The boundary is the max slot covered by completed Parquet exports</li>
 * </ol>
 *
 * <p>Views are refreshed periodically to pick up newly exported Parquet partitions / newly
 * committed DuckLake files and to advance the Parquet/PostgreSQL boundary. After initialization
 * the DuckDB instance is locked down to the analytics export directory
 * ({@link DuckDbReadConnectionProvider#lockDown(Path)}); all file references built here are
 * absolute paths inside that directory.</p>
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.query", name = "enabled", havingValue = "true")
public class ParquetReadConnectionProvider extends DuckDbReadConnectionProvider {

    static final String PG_LIVE_ALIAS = "pg_live";

    private final ParquetTableRegistry tableRegistry;
    private final AnalyticsStoreProperties properties;
    private final ObjectProvider<DuckDbConnectionHelper> connectionHelperProvider;
    private final ObjectProvider<CutoffSlotResolver> cutoffResolverProvider;
    private final ObjectProvider<TableExporterRegistry> exporterRegistryProvider;
    private final ObjectProvider<DuckLakeCatalogSnapshotReader> snapshotReaderProvider;

    private volatile boolean liveDataActive = false;
    private volatile String pgSchema;
    /** Tables currently served by a unified (Parquet UNION ALL live PostgreSQL) view. */
    private volatile Set<String> federatedTables = Set.of();

    public ParquetReadConnectionProvider(
            AnalyticsStoreProperties properties,
            ParquetTableRegistry tableRegistry,
            ObjectProvider<DuckDbConnectionHelper> connectionHelperProvider,
            ObjectProvider<CutoffSlotResolver> cutoffResolverProvider,
            ObjectProvider<TableExporterRegistry> exporterRegistryProvider,
            ObjectProvider<DuckLakeCatalogSnapshotReader> snapshotReaderProvider) {
        super(
                properties.getDuckdb().getReader().getMaximumPoolSize(),
                properties.getDuckdb().getMemoryLimit(),
                properties.getDuckdb().getThreads(),
                properties.getDuckdb().getReader().getQueryTimeoutSeconds()
        );
        this.properties = properties;
        this.tableRegistry = tableRegistry;
        this.connectionHelperProvider = connectionHelperProvider;
        this.cutoffResolverProvider = cutoffResolverProvider;
        this.exporterRegistryProvider = exporterRegistryProvider;
        this.snapshotReaderProvider = snapshotReaderProvider;
    }

    @PostConstruct
    void createViews() {
        List<String> tables = discoverTables().orElse(tableRegistry.getTableNames());
        if (tables.isEmpty()) {
            log.warn("No analytics tables discovered. Scheduled refresh will keep checking.");
        }

        boolean liveDataEnabled = properties.getQuery().isLiveDataEnabled();

        // Step 1: Create Parquet views
        // If live data is enabled, use "parquet_" prefix; otherwise use the table name directly
        int created = 0;
        for (String table : tables) {
            try {
                String viewName = liveDataEnabled ? "parquet_" + table : table;
                if (createHistoricalView(viewName, table)) {
                    created++;
                }
            } catch (SQLException e) {
                log.error("Failed to create historical view for table '{}': {}",
                        table, DuckDbConnectionHelper.redactSecrets(e.getMessage()));
            }
        }
        log.info("Created {} DuckDB Parquet views", created);

        // Step 2: If live data enabled, install postgres_scanner and create unified views
        if (liveDataEnabled) {
            try {
                setupLiveDataFederation(tables);
            } catch (SQLException e) {
                log.error("Failed to attach live PostgreSQL ({}); falling back to historical-only mode",
                        DuckDbConnectionHelper.redactSecrets(e.getMessage()));
                // Fallback: rename parquet_ views back to their original names
                fallbackToParquetOnly(tables);
            }
        }

        // Step 3: Lock down (AFTER postgres_scanner is installed)
        lockDownAnalyticsDirectory();
    }

    /**
     * Periodically refresh views to pick up newly exported Parquet partitions
     * and advance the Parquet/PostgreSQL boundary.
     *
     * <p>Runs every 5 minutes. The Parquet views are refreshed to include new
     * partition files (or newly committed DuckLake files), and unified views are
     * recreated with updated cutoff slots. When the DuckLake catalog cannot be read
     * right now (an export holds the writer connection), the current views are kept
     * unchanged and the refresh is retried on the next run.</p>
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000) // 5 minutes
    public void refreshViews() {
        Optional<List<String>> discovered = discoverTables();
        if (discovered.isEmpty()) {
            log.debug("Table discovery deferred; keeping current views");
            return;
        }
        List<String> tables = discovered.get();
        boolean liveRequested = properties.getQuery().isLiveDataEnabled();
        for (String table : tables) {
            try {
                String viewName = liveRequested ? "parquet_" + table : table;
                createHistoricalView(viewName, table);
            } catch (SQLException e) {
                log.error("Failed to refresh historical view for table '{}': {}",
                        table, DuckDbConnectionHelper.redactSecrets(e.getMessage()));
            }
        }

        if (liveDataActive) {
            refreshUnifiedViews();
        } else if (liveRequested) {
            fallbackToParquetOnly(tables);
        }

        log.debug("Refreshed views for {} tables (liveData={})", tables.size(), liveDataActive);
    }

    /**
     * Whether live PostgreSQL federation is active.
     */
    public boolean isLiveDataActive() {
        return liveDataActive;
    }

    /**
     * True if the table's view currently unions historical data with live PostgreSQL rows
     * (i.e. reaches the chain tip); false if it serves the exported data only.
     */
    public boolean isFederated(String table) {
        return federatedTables.contains(table);
    }

    // ========== Private Implementation ==========

    private void setupLiveDataFederation(List<String> tables) throws SQLException {
        DuckDbConnectionHelper helper = connectionHelperProvider.getIfAvailable();
        CutoffSlotResolver cutoffResolver = cutoffResolverProvider.getIfAvailable();

        if (helper == null) {
            log.warn("DuckDbConnectionHelper not available. Live data federation disabled.");
            fallbackToParquetOnly(tables);
            return;
        }
        if (cutoffResolver == null) {
            log.warn("CutoffSlotResolver not available. Live data federation disabled.");
            fallbackToParquetOnly(tables);
            return;
        }

        // Attachment failures are global. Individual table failures are isolated below.
        helper.installPostgresScanner(getParentConnection());

        // Attach PostgreSQL as pg_live (READ_ONLY, with statement_timeout)
        int pgTimeout = properties.getQuery().getPostgresStatementTimeoutSeconds();
        helper.attachSourceDatabase(getParentConnection(), PG_LIVE_ALIAS, pgTimeout);

        // Resolve the PostgreSQL schema name
        this.pgSchema = helper.getSourceCredentials().getSchema();
        if (pgSchema == null || pgSchema.isBlank()) {
            pgSchema = "public";
        }

        log.info("PostgreSQL attached as '{}' (schema: {})", PG_LIVE_ALIAS, pgSchema);
        this.liveDataActive = true;

        // Create unified views for each table
        Set<String> excludedTables = properties.getQuery().getLiveDataExcludedTables();
        Set<String> federated = new java.util.HashSet<>();
        int unified = 0;
        int skipped = 0;

        for (String table : tables) {
            try {
                if (excludedTables.contains(table)) {
                    // Use Parquet-only: create alias view without prefix
                    createAliasView(table, "parquet_" + table);
                    skipped++;
                    continue;
                }

                long cutoff = cutoffResolver.getCutoffSlot(table);
                String sql = UnifiedViewBuilder.buildUnifiedViewSql(
                        table, PG_LIVE_ALIAS, pgSchema, cutoff,
                        partitionColumnFor(table), getParentConnection());

                if (sql != null) {
                    executeOnParent(sql);
                    unified++;
                    federated.add(table);
                    log.debug("Created unified view '{}' (cutoff slot: {})", table, cutoff);
                } else {
                    // Cannot federate — use Parquet-only alias
                    createAliasView(table, "parquet_" + table);
                    skipped++;
                }
            } catch (Exception e) {
                skipped++;
                logTableFailure("Live federation failed", table, e);
                createFallbackAliasSafely(table);
            }
        }

        this.federatedTables = Set.copyOf(federated);
        log.info("Live data federation active: {} unified views, {} historical-only", unified, skipped);
    }

    private void refreshUnifiedViews() {
        CutoffSlotResolver cutoffResolver = cutoffResolverProvider.getIfAvailable();
        if (cutoffResolver == null) {
            log.warn("CutoffSlotResolver not available during refresh");
            return;
        }

        // Refresh cutoff slots
        cutoffResolver.refresh();

        // Verify PostgreSQL attachment is healthy
        if (!verifyPgAttachment()) {
            // The DuckDB instance is intentionally locked after initialization. Reattaching an
            // external database would require weakening enable_external_access, so fail closed
            // and let the service lifecycle recreate the provider instead.
            log.error("PostgreSQL attachment unavailable during refresh. DuckDB is security-locked; " +
                    "restart the service to recreate live-data federation.");
            return;
        }

        // Recreate unified views with updated cutoffs
        Set<String> excludedTables = properties.getQuery().getLiveDataExcludedTables();
        Set<String> federated = new java.util.HashSet<>();
        for (String table : tableRegistry.getTableNames()) {
            if (excludedTables.contains(table)) {
                createFallbackAliasSafely(table);
                continue;
            }

            try {
                long cutoff = cutoffResolver.getCutoffSlot(table);
                String sql = UnifiedViewBuilder.buildUnifiedViewSql(
                        table, PG_LIVE_ALIAS, pgSchema, cutoff,
                        partitionColumnFor(table), getParentConnection());
                if (sql != null) {
                    executeOnParent(sql);
                    federated.add(table);
                } else {
                    createFallbackAliasSafely(table);
                }
            } catch (Exception e) {
                logTableFailure("Failed to refresh unified view", table, e);
                createFallbackAliasSafely(table);
            }
        }
        this.federatedTables = Set.copyOf(federated);
    }

    /**
     * Verify the PostgreSQL attachment is still healthy.
     */
    private boolean verifyPgAttachment() {
        try (Statement stmt = getParentConnection().createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT database_name FROM duckdb_databases() WHERE database_name = '" + PG_LIVE_ALIAS + "'")) {
            return rs.next();
        } catch (SQLException e) {
            log.debug("PostgreSQL attachment health check failed: {}",
                    DuckDbConnectionHelper.redactSecrets(e.getMessage()));
            return false;
        }
    }

    /**
     * Fall back to Parquet-only mode by creating alias views from parquet_ prefixed views.
     */
    private void fallbackToParquetOnly(List<String> tables) {
        for (String table : tables) {
            try {
                createAliasView(table, "parquet_" + table);
            } catch (SQLException e) {
                log.error("Failed to create fallback alias for '{}': {}",
                        table, DuckDbConnectionHelper.redactSecrets(e.getMessage()));
            }
        }
        this.liveDataActive = false;
        this.federatedTables = Set.of();
    }

    /**
     * Create (or replace) the historical view for a table.
     *
     * @return true if the view was created, false if the table currently has no data files
     *         (DuckLake table without committed files) and no view was created
     */
    private boolean createHistoricalView(String viewName, String tableName) throws SQLException {
        String source;
        String projection = "*";
        if (tableRegistry.isDuckLake()) {
            List<Path> files = tableRegistry.getDuckLakeFiles(tableName);
            if (files.isEmpty()) {
                log.debug("No committed DuckLake data files for '{}'; view not created", tableName);
                return false;
            }
            // Explicit list of committed files (absolute paths inside the sandboxed export dir).
            // DuckLake writes partition columns into the files themselves; hive_partitioning
            // additionally lets DuckDB prune files by the date=/epoch= directory names — but
            // only when every file shares the same layout (DuckDB rejects a mix of
            // partitioned and unpartitioned files with "Hive partition mismatch"; DuckLake can
            // produce such a mix, e.g. when it flushes inlined data or after compaction).
            boolean hivePartitioning = hasUniformHiveLayout(files);
            String hiveTypes = "";
            if (!hivePartitioning) {
                log.info("DuckLake table '{}' mixes partitioned and unpartitioned data files; "
                        + "hive partition pruning disabled for its view", tableName);
            } else {
                hiveTypes = hiveTypesClause(files.get(0));
            }
            String fileList = files.stream()
                    .map(path -> "'" + escapePath(path.toString()) + "'")
                    .collect(Collectors.joining(", "));
            source = "read_parquet([" + fileList + "], hive_partitioning=" + hivePartitioning + hiveTypes + ")";
            // Newer DuckLake versions may store internal bookkeeping columns in some data
            // files (e.g. _ducklake_internal_row_id); they are not part of the table.
            projection = "COLUMNS(c -> NOT starts_with(c, '_ducklake_internal_'))";
        } else {
            String parquetPath = tableRegistry.getParquetGlobPath(tableName);
            source = "read_parquet('" + escapePath(parquetPath) + "', hive_partitioning=true)";
        }
        String sql = "CREATE OR REPLACE VIEW " + quoteIdentifier(viewName)
                + " AS SELECT " + projection + " FROM " + source;
        executeOnParent(sql);
        log.debug("Created historical view '{}'", viewName);
        return true;
    }

    /**
     * True when all files carry the same hive partition keys in their directory path
     * (e.g. every file sits under a {@code date=...} directory, or none does).
     */
    static boolean hasUniformHiveLayout(List<Path> files) {
        Set<List<String>> layouts = new java.util.HashSet<>();
        for (Path file : files) {
            layouts.add(hiveKeys(file));
            if (layouts.size() > 1) {
                return false;
            }
        }
        return true;
    }

    /** Hive partition keys ({@code key=value} directory names) on a file's path, in order. */
    static List<String> hiveKeys(Path file) {
        List<String> keys = new java.util.ArrayList<>();
        Path parent = file.getParent();
        if (parent != null) {
            for (Path segment : parent) {
                String name = segment.toString();
                int eq = name.indexOf('=');
                if (eq > 0) {
                    keys.add(name.substring(0, eq));
                }
            }
        }
        return keys;
    }

    /**
     * Build a {@code , hive_types={...}} clause pinning each hive partition key to the type the
     * column has <em>inside</em> the DuckLake data files.
     *
     * <p>DuckLake writes the partition column into the files (e.g. {@code epoch INTEGER}) while
     * DuckDB auto-types the {@code epoch=...} directory value as BIGINT. With the two types
     * disagreeing, DuckDB 1.5.x fails aggregate queries on that column with an internal
     * "inequal types (BIGINT != INTEGER)" error. Declaring the hive type explicitly keeps
     * partition pruning and makes the view column type match the file. Best effort: on any
     * problem the clause is omitted and DuckDB's auto-detection is used.</p>
     */
    private String hiveTypesClause(Path sampleFile) {
        List<String> keys = hiveKeys(sampleFile);
        if (keys.isEmpty()) {
            return "";
        }
        Map<String, String> fileTypes = new java.util.LinkedHashMap<>();
        String describe = "DESCRIBE SELECT * FROM read_parquet(['" + escapePath(sampleFile.toString())
                + "'], hive_partitioning=false)";
        try (Statement stmt = getParentConnection().createStatement();
             ResultSet rs = stmt.executeQuery(describe)) {
            while (rs.next()) {
                fileTypes.put(rs.getString("column_name"), rs.getString("column_type"));
            }
        } catch (SQLException e) {
            log.debug("Could not read Parquet schema for hive_types ({}); using DuckDB's hive type auto-detection",
                    DuckDbConnectionHelper.redactSecrets(e.getMessage()));
            return "";
        }
        String types = keys.stream()
                .filter(fileTypes::containsKey)
                .map(key -> "'" + key.replace("'", "''") + "': " + fileTypes.get(key))
                .collect(Collectors.joining(", "));
        return types.isEmpty() ? "" : ", hive_types={" + types + "}";
    }

    private void lockDownAnalyticsDirectory() {
        lockDown(Path.of(properties.getExportPath()));
    }

    /**
     * Discover the queryable tables.
     *
     * @return the current table names, or {@link Optional#empty()} when discovery could not
     *         run right now (DuckLake catalog temporarily unavailable) and the caller should
     *         keep its current state
     */
    private Optional<List<String>> discoverTables() {
        if (!tableRegistry.isDuckLake()) {
            tableRegistry.refresh();
            return Optional.of(tableRegistry.getTableNames());
        }

        DuckLakeCatalogSnapshotReader snapshotReader = snapshotReaderProvider.getIfAvailable();
        if (snapshotReader == null) {
            log.warn("DuckLake catalog snapshot reader is unavailable (requires yaci.store.analytics.enabled=true "
                    + "in this process); no DuckLake tables can be queried");
            return Optional.of(tableRegistry.getTableNames());
        }
        try {
            Optional<Map<String, DuckLakeCatalogSnapshotReader.TableFiles>> snapshot =
                    snapshotReader.readSnapshot();
            if (snapshot.isEmpty()) {
                return Optional.empty();
            }
            Map<String, List<Path>> filesByTable = snapshot.get().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().dataFiles()));
            List<String> before = tableRegistry.getTableNames();
            tableRegistry.replaceDuckLakeSnapshot(filesByTable);
            List<String> after = tableRegistry.getTableNames();
            if (!before.equals(after)) {
                log.info("Discovered {} DuckLake tables with committed data: {}", after.size(), after);
            } else {
                log.debug("DuckLake snapshot refreshed for {} tables", after.size());
            }
            return Optional.of(after);
        } catch (SQLException e) {
            log.error("Failed to read DuckLake catalog snapshot: {}",
                    DuckDbConnectionHelper.redactSecrets(e.getMessage()));
            return Optional.of(tableRegistry.getTableNames());
        }
    }

    private String partitionColumnFor(String table) {
        TableExporterRegistry registry = exporterRegistryProvider.getIfAvailable();
        if (registry != null && registry.hasExporter(table)) {
            return registry.getExporter(table).getPartitionColumn();
        }
        return "block_time";
    }

    private void createFallbackAliasSafely(String table) {
        try {
            createAliasView(table, "parquet_" + table);
        } catch (SQLException aliasError) {
            log.error("Failed to create historical fallback for '{}': {}",
                    table, DuckDbConnectionHelper.redactSecrets(aliasError.getMessage()));
        }
    }

    private void logTableFailure(String action, String table, Exception error) {
        // Messages may carry postgres_scanner connection strings; log them redacted.
        log.error("{} for '{}' ({}: {}); using historical data",
                action, table, error.getClass().getSimpleName(),
                DuckDbConnectionHelper.redactSecrets(error.getMessage()));
    }

    private void createAliasView(String viewName, String sourceView) throws SQLException {
        String sql = "CREATE OR REPLACE VIEW " + quoteIdentifier(viewName)
                + " AS SELECT * FROM " + quoteIdentifier(sourceView);
        executeOnParent(sql);
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String escapePath(String path) {
        return path.replace("'", "''");
    }
}
