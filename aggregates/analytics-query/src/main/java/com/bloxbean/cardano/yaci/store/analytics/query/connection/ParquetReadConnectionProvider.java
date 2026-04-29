package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbReadConnectionProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

/**
 * DuckDB connection provider that creates views over Parquet files and optionally
 * federates live PostgreSQL data via {@code postgres_scanner}.
 *
 * <p>On startup, creates a DuckDB view for each table discovered by {@link ParquetTableRegistry},
 * pointing to the Parquet files via {@code read_parquet()} with hive partitioning enabled.</p>
 *
 * <p>When {@code yaci.store.analytics.query.live-data-enabled=true}, the provider also:</p>
 * <ol>
 *   <li>Installs the {@code postgres_scanner} extension</li>
 *   <li>Attaches the source PostgreSQL database as {@code pg_live}</li>
 *   <li>Creates unified views that UNION ALL Parquet (historical) with PostgreSQL (live)</li>
 *   <li>The boundary is the max slot covered by completed Parquet exports</li>
 * </ol>
 *
 * <p>Views are refreshed periodically to pick up newly exported Parquet partitions
 * and advance the Parquet/PostgreSQL boundary.</p>
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

    private volatile boolean liveDataActive = false;
    private volatile String pgSchema;

    public ParquetReadConnectionProvider(
            AnalyticsStoreProperties properties,
            ParquetTableRegistry tableRegistry,
            ObjectProvider<DuckDbConnectionHelper> connectionHelperProvider,
            ObjectProvider<CutoffSlotResolver> cutoffResolverProvider) {
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
    }

    @PostConstruct
    void createViews() {
        List<String> tables = tableRegistry.getTableNames();
        if (tables.isEmpty()) {
            log.warn("No Parquet tables discovered. Analytics query layer will have no data.");
            lockDown();
            return;
        }

        boolean liveDataEnabled = properties.getQuery().isLiveDataEnabled();

        // Step 1: Create Parquet views
        // If live data is enabled, use "parquet_" prefix; otherwise use the table name directly
        int created = 0;
        for (String table : tables) {
            try {
                String viewName = liveDataEnabled ? "parquet_" + table : table;
                createParquetView(viewName, table);
                created++;
            } catch (SQLException e) {
                log.error("Failed to create Parquet view for table '{}': {}", table, e.getMessage());
            }
        }
        log.info("Created {} DuckDB Parquet views", created);

        // Step 2: If live data enabled, install postgres_scanner and create unified views
        if (liveDataEnabled) {
            try {
                setupLiveDataFederation(tables);
            } catch (Exception e) {
                log.error("Failed to set up live data federation. Falling back to Parquet-only mode.", e);
                // Fallback: rename parquet_ views back to their original names
                fallbackToParquetOnly(tables);
            }
        }

        // Step 3: Lock down (AFTER postgres_scanner is installed)
        lockDown();
    }

    /**
     * Periodically refresh views to pick up newly exported Parquet partitions
     * and advance the Parquet/PostgreSQL boundary.
     *
     * <p>Runs every 5 minutes. The Parquet views are refreshed to include new
     * partition files, and unified views are recreated with updated cutoff slots.</p>
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000) // 5 minutes
    public void refreshViews() {
        for (String table : tableRegistry.getTableNames()) {
            try {
                String viewName = liveDataActive ? "parquet_" + table : table;
                createParquetView(viewName, table);
            } catch (SQLException e) {
                log.error("Failed to refresh Parquet view for table '{}': {}", table, e.getMessage());
            }
        }

        if (liveDataActive) {
            refreshUnifiedViews();
        }

        log.debug("Refreshed views for {} tables (liveData={})",
                tableRegistry.getTableNames().size(), liveDataActive);
    }

    /**
     * Whether live PostgreSQL federation is active.
     */
    public boolean isLiveDataActive() {
        return liveDataActive;
    }

    // ========== Private Implementation ==========

    private void setupLiveDataFederation(List<String> tables) {
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

        try {
            // Install postgres_scanner on the parent connection
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

            // Create unified views for each table
            Set<String> excludedTables = properties.getQuery().getLiveDataExcludedTables();
            int unified = 0;
            int skipped = 0;

            for (String table : tables) {
                if (excludedTables.contains(table)) {
                    // Use Parquet-only: create alias view without prefix
                    createAliasView(table, "parquet_" + table);
                    skipped++;
                    continue;
                }

                long cutoff = cutoffResolver.getCutoffSlot(table);
                String sql = UnifiedViewBuilder.buildUnifiedViewSql(
                        table, PG_LIVE_ALIAS, pgSchema, cutoff, getParentConnection());

                if (sql != null) {
                    executeOnParent(sql);
                    unified++;
                    log.debug("Created unified view '{}' (cutoff slot: {})", table, cutoff);
                } else {
                    // Cannot federate — use Parquet-only alias
                    createAliasView(table, "parquet_" + table);
                    skipped++;
                }
            }

            this.liveDataActive = true;
            log.info("Live data federation active: {} unified views, {} Parquet-only", unified, skipped);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to set up postgres_scanner federation", e);
        }
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
            log.warn("PostgreSQL attachment unhealthy during refresh. Attempting reconnect...");
            if (!reconnectPostgres()) {
                log.error("PostgreSQL reconnect failed. Unified views may serve stale data.");
                return;
            }
        }

        // Recreate unified views with updated cutoffs
        Set<String> excludedTables = properties.getQuery().getLiveDataExcludedTables();
        for (String table : tableRegistry.getTableNames()) {
            if (excludedTables.contains(table)) continue;

            try {
                long cutoff = cutoffResolver.getCutoffSlot(table);
                String sql = UnifiedViewBuilder.buildUnifiedViewSql(
                        table, PG_LIVE_ALIAS, pgSchema, cutoff, getParentConnection());
                if (sql != null) {
                    executeOnParent(sql);
                }
            } catch (SQLException e) {
                log.error("Failed to refresh unified view for '{}': {}", table, e.getMessage());
            }
        }
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
            log.debug("PostgreSQL attachment health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Attempt to reconnect PostgreSQL after a connection failure.
     */
    private boolean reconnectPostgres() {
        DuckDbConnectionHelper helper = connectionHelperProvider.getIfAvailable();
        if (helper == null) return false;

        try {
            // Detach stale connection
            try {
                executeOnParent("DETACH IF EXISTS " + PG_LIVE_ALIAS);
            } catch (SQLException ignored) {
            }

            // Reattach with statement_timeout
            int pgTimeout = properties.getQuery().getPostgresStatementTimeoutSeconds();
            helper.attachSourceDatabase(getParentConnection(), PG_LIVE_ALIAS, pgTimeout);
            log.info("PostgreSQL reconnected as '{}'", PG_LIVE_ALIAS);
            return true;
        } catch (SQLException e) {
            log.error("Failed to reconnect PostgreSQL: {}", e.getMessage());
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
                log.error("Failed to create fallback alias view for '{}': {}", table, e.getMessage());
            }
        }
        this.liveDataActive = false;
    }

    private void createParquetView(String viewName, String tableName) throws SQLException {
        String parquetPath = tableRegistry.getParquetGlobPath(tableName);
        String sql = "CREATE OR REPLACE VIEW " + quoteIdentifier(viewName)
                + " AS SELECT * FROM read_parquet('" + escapePath(parquetPath)
                + "', hive_partitioning=true)";
        executeOnParent(sql);
        log.debug("Created Parquet view: {} -> {}", viewName, parquetPath);
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
