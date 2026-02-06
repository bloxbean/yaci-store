package com.bloxbean.cardano.yaci.store.analytics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

@ConfigurationProperties(prefix = "yaci.store.analytics")
@Data
public class AnalyticsStoreProperties {

    private boolean enabled = false;
    private String exportPath = "./data/analytics";
    private String dailyExportCron = "0 0 0 * * *";
    private String epochExportCron = "0 0 1 * * *";

    /**
     * Finalization lag in days (default: 2).
     *
     * Ensures exported data is immutable by exporting data that is N days old.
     * This accounts for Cardano's 2160 block security parameter (~12 hours)
     * plus additional buffer for reorgs.
     *
     * Examples:
     * - finalizationLagDays=2: Export data from 2 days ago
     * - If today is 2024-01-17, exports data for 2024-01-15
     */
    private int finalizationLagDays = 2;

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

    private StateManagement stateManagement = new StateManagement();
    private Verification verification = new Verification();
    private ContinuousSync continuousSync = new ContinuousSync();
    private Admin admin = new Admin();
    private Storage storage = new Storage();
    private DuckDb duckdb = new DuckDb();
    private ParquetExport parquetExport = new ParquetExport();
    private DuckLake ducklake = new DuckLake();

    @Data
    public static class StateManagement {
        private boolean enabled = true;
        private int staleTimeoutMinutes = 60;
        private int maxRetries = 3;
    }

    @Data
    public static class Verification {
        private boolean enabled = true;
        private String cron = "0 0 2 * * MON";
    }

    @Data
    public static class ContinuousSync {
        private int bufferDays = 3;
        private int syncCheckIntervalMinutes = 15;
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
        private DuckDbDataSource datasource = new DuckDbDataSource();
        private ReaderConfig reader = new ReaderConfig();

        @Data
        public static class DuckDbDataSource {
            private HikariConfig hikari = new HikariConfig();

            @Data
            public static class HikariConfig {
                private int maximumPoolSize = 2;
                private int minimumIdle = 1;
                private long connectionTimeout = 30000;
                private long idleTimeout = 600000;
                private long maxLifetime = 1800000;
            }
        }

        @Data
        public static class ReaderConfig {
            /**
             * Maximum pool size for DuckDB reader DataSource.
             *
             * Defaults to available processor cores for optimal concurrent query throughput.
             * DuckDB supports multiple concurrent readers even on file-based catalogs.
             */
            private int maximumPoolSize = Runtime.getRuntime().availableProcessors();
        }
    }

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
}
