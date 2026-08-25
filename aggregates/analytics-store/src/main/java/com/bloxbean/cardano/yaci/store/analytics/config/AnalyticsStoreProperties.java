package com.bloxbean.cardano.yaci.store.analytics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "yaci.store.analytics")
@Data
public class AnalyticsStoreProperties {

    private boolean enabled = false;
    private String exportPath = "./data/analytics";

    /**
     * Enabled tables for export (empty = all enabled).
     *
     * If empty, all registered table exporters are enabled.
     * If specified, only listed tables will be exported.
     *
     * Example:
     * enabled-tables=transactions,transaction_outputs,spent_outputs,address_balance
     */
    private Set<String> enabledTables = new HashSet<>();

    private List<CustomExporterConfig> customExporters = new ArrayList<>();

    /**
     * Per-exporter enable/disable flags (enabled by default).
     *
     * Key = table name (e.g., "reward", "epoch_stake").
     * Example:
     *   yaci.store.analytics.exporter.reward.enabled=false
     *   yaci.store.analytics.exporter.epoch_stake.enabled=false
     */
    private Map<String, ExporterConfig> exporter = new HashMap<>();

    @Data
    public static class ExporterConfig {
        private boolean enabled = true;
    }

    private StateManagement stateManagement = new StateManagement();
    private ContinuousSync continuousSync = new ContinuousSync();
    private Admin admin = new Admin();
    private Storage storage = new Storage();
    private DuckDb duckdb = new DuckDb();
    private ParquetExport parquetExport = new ParquetExport();
    private DuckLake ducklake = new DuckLake();

    @Data
    public static class StateManagement {
        private int staleTimeoutMinutes = 60;
    }

    @Data
    public static class ContinuousSync {
        /**
         * How many UTC days behind the <em>finalized</em> tip the daily exports stay. The
         * finalized tip is the latest synced block time minus the finality window (see
         * {@link #finalityMarginHours}); a day partition D is exported once the finalized tip's
         * UTC date is at least D + bufferDays. With the default 1 on mainnet (12 h window + 1 h
         * margin) a day is exported at 13:00 UTC of the following day: its newest rows are
         * 13 h old, its oldest 37 h. Each additional day adds 24 h.
         *
         * Minimum 1 — a value of 0 would export the (still incomplete) day the finalized tip
         * is in and mark it completed, so smaller values are raised to 1 with a warning at
         * startup. Default: 1.
         */
        private int bufferDays = 1;

        /**
         * Extra hours added on top of the finality window derived from the network's genesis
         * (security parameter k blocks × expected block interval, i.e.
         * {@code k × slotLength / activeSlotsCoeff}: 12 h on mainnet and preprod, ~2.4 h on
         * preview). The window guarantees that every exported row is older than Cardano's
         * rollback bound (k blocks; the window is their expected production time); the margin
         * covers the gap-detection interval and clock skew. Negative values are treated as 0.
         * Default: 1.
         */
        private int finalityMarginHours = 1;

        /**
         * Override for the derived finality window, in hours ({@code 0} = derive from genesis).
         * Only needed for custom networks whose genesis parameters are not available, or for
         * tests. The margin is added on top in either case.
         */
        private int finalityWindowHours = 0;
        /** Gap-detection interval when fully synced (minutes). Default: 15. */
        private int syncCheckIntervalMinutes = 15;
        /** Gap-detection interval while exports are behind (minutes). Default: 1. */
        private int catchUpIntervalMinutes = 1;

        /**
         * When true (default), analytics exports are deferred until the sync reaches chain tip.
         * During initial sync (block range sync mode), all exports are skipped.
         * Set to false to allow exports during sync
         */
        private boolean exportAfterSync = true;
    }

    @Data
    public static class Admin {
        private boolean enabled = false;
    }

    @Data
    public static class Storage {
        /**
         * Storage format type: "parquet" or "ducklake"
         *
         * - parquet: Direct Parquet file export (default, backward compatible)
         * - ducklake: DuckLake-managed Parquet files with catalog metadata
         *
         * DuckLake provides:
         * - ACID transactions
         * - Time-travel queries
         * - Schema evolution
         * - Catalog metadata (PostgreSQL or DuckDB)
         */
        private String type = "parquet";
    }

    @Data
    public static class DuckDb {
        /**
         * DuckDB memory_limit setting.
         * Controls the maximum memory DuckDB's buffer manager can use.
         * DuckDB defaults to 80% of system RAM if not set, which can cause OOM
         * when running inside a JVM process.
         *
         * Format: DuckDB size string (e.g., "1GB", "512MB", "2GB")
         * Empty = use DuckDB default (80% of system RAM).
         * Recommended: Set explicitly in production/container environments.
         */
        private String memoryLimit;

        /**
         * Number of DuckDB threads for query execution.
         * Controls parallelism within DuckDB's query engine.
         *
         * Defaults to available processor cores.
         * Lower values prevent one query from monopolizing all CPU cores.
         */
        private int threads = Runtime.getRuntime().availableProcessors();

        private ReaderConfig reader = new ReaderConfig();

        @Data
        public static class ReaderConfig {
            /**
             * Maximum concurrent DuckDB read queries.
             *
             * Controls the semaphore size for DuckDBConnection.duplicate() based readers
             * (the analytics query layer).
             * Defaults to available processor cores.
             */
            private int maximumPoolSize = Runtime.getRuntime().availableProcessors();

            /**
             * Per-query timeout in seconds.
             * Queries exceeding this timeout are cancelled.
             */
            private int queryTimeoutSeconds = 30;
        }
    }

    /**
     * Analytics query layer configuration.
     * Controls the DuckDB-based query engine that reads Parquet files directly.
     */
    @Data
    public static class Query {
        private boolean enabled = false;

        /**
         * Enable live PostgreSQL data federation via DuckDB's postgres_scanner.
         *
         * When enabled, unified DuckDB views are created that UNION ALL historical
         * Parquet data with live PostgreSQL data. The boundary is determined automatically
         * from the last completed Parquet export.
         *
         * This allows MCP clients to query a single unified view spanning from genesis
         * to the current chain tip, without needing to know about the data boundary.
         *
         * Requires: postgres_scanner DuckDB extension (installed automatically).
         * Default: false (backward compatible — Parquet-only mode).
         */
        private boolean liveDataEnabled = false;

        /**
         * Tables to exclude from live PostgreSQL federation.
         *
         * These tables will remain Parquet-only even when live-data-enabled=true.
         * Useful for tables where postgres_scanner performance is poor or where
         * live data is not needed (e.g., epoch-partitioned reference data).
         *
         * Example: yaci.store.analytics.query.live-data-excluded-tables=epoch_stake,reward
         */
        private Set<String> liveDataExcludedTables = new HashSet<>();

        /**
         * PostgreSQL statement timeout for analytics queries via postgres_scanner (in seconds).
         *
         * Applied as a PostgreSQL connection parameter ({@code statement_timeout}) on the
         * attached {@code pg_live} database. Prevents heavy analytical queries from overloading
         * the PostgreSQL server that is also used for blockchain sync.
         *
         * Only applies when {@code live-data-enabled=true}. Does not affect the DuckDB
         * query timeout (controlled by {@code duckdb.reader.query-timeout-seconds}).
         *
         * Default: 30 seconds.
         */
        private int postgresStatementTimeoutSeconds = 30;

        /**
         * Rows returned by an ad-hoc SQL request when the caller does not specify
         * {@code maxRows}. The query is executed as {@code SELECT * FROM (<sql>) LIMIT n+1}
         * (the way SQL front-ends such as Superset or the DuckDB UI cap results), so DuckDB
         * stops producing rows at the limit instead of materializing the whole result; the
         * response signals truncation via the {@code X-Analytics-Truncated} header.
         *
         * Values above {@code max-rows} are reduced to it. Default: 100.
         */
        private int defaultMaxRows = 100;

        /**
         * Hard upper bound for rows returned by any single query of the query layer — ad-hoc
         * SQL (whatever {@code maxRows} the caller asks for), the pre-built endpoints and the
         * MCP tools. Protects the JVM heap and the HTTP response size.
         *
         * Can only lower the built-in ceiling of 10,000 rows, not raise it (values above
         * 10,000 or non-positive values are treated as 10,000). Default: 10,000.
         */
        private int maxRows = 10_000;

        /**
         * Enable/disable the REST API endpoints for analytics queries.
         *
         * When false, only the MCP tools are available for querying analytics data.
         * The REST endpoints ({@code /api/v1/analytics/query/*}) will not be registered.
         *
         * Useful for production deployments where analytics should only be accessible
         * through the MCP interface (gated by the LLM agent), not via direct HTTP calls.
         *
         * Default: false. Ad-hoc SQL must be explicitly exposed by the operator.
         */
        private boolean restApiEnabled = false;
    }

    private Query query = new Query();

    @Data
    public static class ParquetExport {
        /**
         * Compression codec for Parquet files.
         * Options: SNAPPY, ZSTD, GZIP, LZ4, BROTLI, UNCOMPRESSED
         *
         * Recommended: ZSTD (best balance of compression ratio and speed)
         * - ZSTD: 35-40% better compression than SNAPPY with minimal speed penalty
         * - SNAPPY: Faster but larger files
         * - GZIP: Better compression but slower queries
         */
        private String codec = "ZSTD";

        /**
         * Compression level (only applicable for ZSTD).
         * Range: 1-22 (default: 3)
         * - 1-3: Fast compression, good for hot data
         * - 4-9: Better compression, good for archival
         * - 10+: Maximum compression, very slow
         */
        private int compressionLevel = 3;

        /**
         * Row group size (number of rows per row group).
         * -1 = use DuckDB default (~122,880 rows or 128MB) - RECOMMENDED
         *
         * Tuning guidance:
         * - Larger values (500K-1M): Better for sequential scans, uses more memory
         * - Smaller values (50K-100K): Better for selective queries, less memory
         * - Default (-1): Good balance for most workloads
         */
        private int rowGroupSize = -1;
    }

    @Data
    public static class DuckLake {
        /**
         * DuckLake catalog type: "postgresql" or "duckdb"
         *
         * - postgresql: Production-grade catalog with multi-instance support
         * - duckdb: Lightweight catalog for development/single-instance deployments
         *
         * PostgreSQL catalog is recommended for:
         * - Production deployments
         * - Multi-writer scenarios
         * - High availability requirements
         *
         * DuckDB catalog is suitable for:
         * - Development/testing
         * - Single-writer deployments
         * - Embedded use cases
         */
        private String catalogType = "postgresql";

        /**
         * PostgreSQL catalog JDBC URL (used when catalogType=postgresql).
         *
         * Example: jdbc:postgresql://localhost:5432/yaci_store
         *
         * Reuses the same PostgreSQL database as yaci-store for metadata.
         * DuckLake creates catalog tables with "ducklake_" prefix.
         */
        private String catalogUrl;

        /**
         * PostgreSQL catalog username (used when catalogType=postgresql).
         *
         * If not specified, uses the main datasource credentials.
         */
        private String catalogUsername;

        /**
         * PostgreSQL catalog password (used when catalogType=postgresql).
         *
         * If not specified, uses the main datasource credentials.
         */
        private String catalogPassword;

        /**
         * DuckDB catalog file path (used when catalogType=duckdb).
         *
         * Example: ./data/analytics/ducklake.catalog.db
         *
         * Stores catalog metadata in a local DuckDB file.
         * Suitable for single-writer deployments only.
         */
        private String catalogPath = "./data/analytics/ducklake.catalog.db";

        // Note: DuckLake always uses 'public' schema for metadata tables
        // catalogSchema is not configurable - removed in favor of hardcoded 'public'

        private DuckLakeExport export = new DuckLakeExport();

        @Data
        public static class DuckLakeExport {
            /**
             * Compression codec for DuckLake Parquet files.
             * Options: SNAPPY, ZSTD, GZIP, LZ4, BROTLI, UNCOMPRESSED
             *
             * Recommended: ZSTD (best balance of compression ratio and speed)
             * - ZSTD: 35-40% better compression than SNAPPY with minimal speed penalty
             * - SNAPPY: Faster but larger files
             * - GZIP: Better compression but slower queries
             *
             * Applied globally to all DuckLake tables via catalog settings.
             */
            private String codec = "ZSTD";

            /**
             * Compression level (only applicable for ZSTD).
             * Range: 1-22 (default: 3)
             * - 1-3: Fast compression, good for hot data
             * - 4-9: Better compression, good for archival
             * - 10+: Maximum compression, very slow
             */
            private int compressionLevel = 3;

            /**
             * Row group size (number of rows per row group).
             * -1 = use DuckDB default (~122,880 rows or 128MB) - RECOMMENDED
             *
             * Tuning guidance:
             * - Larger values (500K-1M): Better for sequential scans, uses more memory
             * - Smaller values (50K-100K): Better for selective queries, less memory
             * - Default (-1): Good balance for most workloads
             */
            private int rowGroupSize = -1;
        }
    }

    @Data
    public static class CustomExporterConfig {
        private String name;
        private String partitionStrategy = "DAILY";
        private String partitionColumn = "block_time";
        private boolean dependsOnAdapotJob = false;
        private String query;
    }
}
