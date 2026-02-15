package com.bloxbean.cardano.yaci.store.analytics.helper;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Helper component for DuckDB connection setup and management.
 *
 * Centralizes common operations shared across ParquetWriterService, DuckLakeWriterService,
 * and DuckLakeCatalogInitializer:
 * - Credential resolution (source vs catalog databases)
 * - Extension installation (postgres_scanner, ducklake)
 * - Database attachment (with connection pooling awareness)
 * - URL parsing utilities
 *
 * This eliminates ~340 lines of duplicated code across services.
 */
@Component
@Slf4j
public class DuckDbConnectionHelper {

    private final Environment environment;
    private final AnalyticsStoreProperties properties;

    // Cached credentials (resolved once at startup)
    private final DatabaseCredentials sourceCredentials;
    private final DatabaseCredentials catalogCredentials;

    /**
     * Immutable credentials container.
     */
    @Value
    @Builder
    public static class DatabaseCredentials {
        String url;
        String username;
        String password;
        String schema;
        String host;
        String port;
        String dbName;
    }

    public DuckDbConnectionHelper(Environment environment, AnalyticsStoreProperties properties) {
        this.environment = environment;
        this.properties = properties;

        // Resolve credentials at startup (cached for performance)
        this.sourceCredentials = resolveSourceCredentials();
        this.catalogCredentials = resolveCatalogCredentials();

        log.debug("Initialized DuckDbConnectionHelper (source schema: {}, catalog URL: {})",
                sourceCredentials.schema,
                catalogCredentials.url.equals(sourceCredentials.url) ? "main datasource" : "custom");
    }

    // ========== Public API ==========

    /**
     * Get source database credentials (always main datasource - where blockchain data lives).
     */
    public DatabaseCredentials getSourceCredentials() {
        return sourceCredentials;
    }

    /**
     * Get catalog database credentials (catalog-specific if provided, else main datasource).
     */
    public DatabaseCredentials getCatalogCredentials() {
        return catalogCredentials;
    }

    /**
     * Install postgres_scanner extension (idempotent).
     */
    public void installPostgresScanner(Connection conn) throws SQLException {
        executeSql(conn, "INSTALL postgres_scanner;");
        executeSql(conn, "LOAD postgres_scanner;");
        log.debug("Installed postgres_scanner extension");
    }

    /**
     * Install ducklake extension (idempotent).
     */
    public void installDuckLake(Connection conn) throws SQLException {
        executeSql(conn, "INSTALL ducklake;");
        executeSql(conn, "LOAD ducklake;");
        log.debug("Installed ducklake extension");
    }

    /**
     * Attach source PostgreSQL database with specified alias.
     * Checks if already attached (connection pooling aware).
     *
     * @param conn DuckDB connection
     * @param aliasName Database alias (e.g., "postgres_db" for Parquet, "source_db" for DuckLake)
     */
    public void attachSourceDatabase(Connection conn, String aliasName) throws SQLException {
        if (isDatabaseAttached(conn, aliasName)) {
            log.debug("Source database '{}' already attached, skipping", aliasName);
            return;
        }

        DatabaseCredentials creds = sourceCredentials;

        StringBuilder cmd = new StringBuilder();
        cmd.append("ATTACH 'dbname=").append(creds.dbName);
        cmd.append(" user=").append(creds.username);
        cmd.append(" password=").append(creds.password);
        cmd.append(" host=").append(creds.host);
        cmd.append(" port=").append(creds.port);

        if (creds.schema != null && !creds.schema.isEmpty()) {
            cmd.append(" options=-csearch_path=").append(creds.schema);
        }

        cmd.append("' AS ").append(aliasName).append(" (TYPE POSTGRES, READ_ONLY);");

        executeSql(conn, cmd.toString());
        log.debug("Attached source PostgreSQL database as '{}'", aliasName);
    }

    /**
     * Check if a database is already attached.
     *
     * @param conn DuckDB connection
     * @param databaseName Database alias name
     * @return true if attached, false otherwise
     */
    public boolean isDatabaseAttached(Connection conn, String databaseName) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(String.format(
                     "SELECT database_name FROM duckdb_databases() WHERE database_name = '%s';",
                     databaseName))) {
            return rs.next();
        } catch (SQLException e) {
            log.debug("Error checking if database '{}' is attached (will attempt attach): {}", databaseName, e.getMessage());
            return false;
        }
    }

    /**
     * Execute SQL statement.
     */
    public void executeSql(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    // ========== DuckLake-Specific Operations ==========

    /**
     * Prepare DuckDB connection for DuckLake operations.
     *
     * This is the main entry point for DuckLake services, handling:
     * - Extension installation (ducklake, postgres_scanner if needed)
     * - Catalog attachment (with optional READ_ONLY mode)
     * - Source database attachment (if needed)
     * - Search path configuration
     * - Database selection (USE ducklake_catalog)
     *
     * @param conn DuckDB connection
     * @param needsSource true if source database attachment is needed (write operations)
     * @param readOnly true to attach catalog in READ_ONLY mode (database-enforced)
     */
    public void prepareConnectionForDuckLake(Connection conn, boolean needsSource, boolean readOnly) throws SQLException {
        // Install DuckLake extension
        installDuckLake(conn);

        // Attach catalog with optional READ_ONLY mode
        attachDuckLakeCatalog(conn, readOnly);

        // Attach source if needed (for write/read operations)
        // This is needed for both PostgreSQL and DuckDB catalog types to read blockchain data
        if (needsSource) {
            installPostgresScanner(conn);
            attachSourceDatabase(conn, "source_db");
        }

        // Use catalog database
        executeSql(conn, "USE ducklake_catalog;");
        log.debug("Using ducklake_catalog (readOnly: {})", readOnly);

        // Set search_path to resolve both CREATE and SELECT operations correctly
        // Order matters: FIRST schema is where CREATE TABLE happens, rest are for SELECT lookups
        // - main: ducklake_catalog's default schema (for CREATE TABLE)
        // - source_db.{schema}: source database schema (for SELECT queries to find existing tables)
        if (needsSource) {
            String searchPath = String.format("main,source_db.%s", sourceCredentials.schema);
            executeSql(conn, String.format("SET search_path = '%s';", searchPath));
            log.debug("Set search_path to: {}", searchPath);
        }
    }

    /**
     * Attach DuckLake catalog (PostgreSQL or DuckDB).
     * Checks if already attached (connection pooling aware).
     *
     * @param conn DuckDB connection
     * @param readOnly If true, attaches catalog in READ_ONLY mode (prevents writes)
     */
    public void attachDuckLakeCatalog(Connection conn, boolean readOnly) throws SQLException {
        String catalogType = properties.getDucklake().getCatalogType();
        String dataPath = properties.getExportPath();

        // Ensure data path directory exists
        Path dataDir = Paths.get(dataPath);
        if (!Files.exists(dataDir)) {
            try {
                Files.createDirectories(dataDir);
            } catch (IOException e) {
                throw new SQLException("Failed to create data path directory: " + e.getMessage(), e);
            }
        }

        // Attach the appropriate catalog type
        // Each attach method handles "already attached" detection via error handling
        if ("postgresql".equalsIgnoreCase(catalogType)) {
            attachPostgresCatalog(conn, dataPath, readOnly);
        } else {
            attachDuckDbCatalog(conn, dataPath, readOnly);
        }
    }

    // ========== URL Parsing Utilities ==========

    /**
     * Extract database name from JDBC URL.
     * Example: jdbc:postgresql://localhost:5432/yaci_store?currentSchema=mainnet → yaci_store
     */
    public String extractDbName(String jdbcUrl) {
        String[] parts = jdbcUrl.split("/");
        String dbPart = parts[parts.length - 1];
        return dbPart.split("\\?")[0]; // Remove query parameters
    }

    /**
     * Extract host from JDBC URL.
     * Example: jdbc:postgresql://localhost:5432/yaci_store → localhost
     */
    public String extractHost(String jdbcUrl) {
        String[] parts = jdbcUrl.split("//")[1].split("/")[0].split(":");
        return parts[0];
    }

    /**
     * Extract port from JDBC URL.
     * Example: jdbc:postgresql://localhost:5432/yaci_store → 5432
     */
    public String extractPort(String jdbcUrl) {
        String[] parts = jdbcUrl.split("//")[1].split("/")[0].split(":");
        return parts.length > 1 ? parts[1] : "5432";
    }

    /**
     * Extract schema from JDBC URL query parameters.
     * Example: jdbc:postgresql://localhost:5432/yaci_store?currentSchema=mainnet → mainnet
     * Default: public
     */
    public String extractSchema(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "public";
        }

        // Look for currentSchema parameter
        if (jdbcUrl.contains("currentSchema=")) {
            String[] parts = jdbcUrl.split("currentSchema=");
            if (parts.length > 1) {
                return parts[1].split("&")[0]; // Handle multiple parameters
            }
        }

        // Default to public schema
        return "public";
    }

    /**
     * Quote SQL identifier with double quotes.
     * Always quotes for safety - handles special characters like hyphens, spaces, etc.
     * Escapes internal double quotes by doubling them (SQL standard).
     *
     * Examples:
     * - "mainnet" → "\"mainnet\""
     * - "preprod-new" → "\"preprod-new\""
     * - "test\"name" → "\"test\"\"name\""
     *
     * @param identifier SQL identifier (schema, table, column name)
     * @return Quoted identifier safe for SQL
     */
    public String quoteIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    // ========== Private Helper Methods ==========

    /**
     * Resolve source database credentials (always main datasource).
     */
    private DatabaseCredentials resolveSourceCredentials() {
        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");

        return DatabaseCredentials.builder()
                .url(url)
                .username(username)
                .password(password)
                .schema(extractSchema(url))
                .host(extractHost(url))
                .port(extractPort(url))
                .dbName(extractDbName(url))
                .build();
    }

    /**
     * Resolve catalog database credentials (catalog-specific if provided, else main datasource).
     */
    private DatabaseCredentials resolveCatalogCredentials() {
        String url = properties.getDucklake().getCatalogUrl() != null ?
                properties.getDucklake().getCatalogUrl() :
                environment.getProperty("spring.datasource.url");

        String username = properties.getDucklake().getCatalogUsername() != null ?
                properties.getDucklake().getCatalogUsername() :
                environment.getProperty("spring.datasource.username");

        String password = properties.getDucklake().getCatalogPassword() != null ?
                properties.getDucklake().getCatalogPassword() :
                environment.getProperty("spring.datasource.password");

        return DatabaseCredentials.builder()
                .url(url)
                .username(username)
                .password(password)
                .schema(extractSchema(url))
                .host(extractHost(url))
                .port(extractPort(url))
                .dbName(extractDbName(url))
                .build();
    }

    /**
     * Attach PostgreSQL-based DuckLake catalog.
     *
     * @param readOnly If true, attaches catalog in READ_ONLY mode
     */
    private void attachPostgresCatalog(Connection conn, String dataPath, boolean readOnly) throws SQLException {
        // Check if already attached by alias name (connection pooling scenario)
        if (isDatabaseAttached(conn, "ducklake_catalog")) {
            log.debug("PostgreSQL DuckLake catalog already attached as 'ducklake_catalog', skipping");
            return;
        }

        DatabaseCredentials creds = catalogCredentials;

        // Note: Schema creation is now handled by DuckLakeCatalogInitializer at startup
        // DuckLake always uses 'public' schema for metadata tables

        String connectionString = String.format(
                "ducklake:postgres:dbname=%s host=%s port=%s user=%s password=%s options=-csearch_path=public",
                creds.dbName,
                creds.host,
                creds.port,
                creds.username,
                creds.password
        );

        // Build ATTACH statement with optional READ_ONLY
        String attachSql;
        if (readOnly) {
            attachSql = String.format(
                    "ATTACH '%s' AS ducklake_catalog (READ_ONLY, DATA_PATH '%s');",
                    connectionString,
                    dataPath
            );
        } else {
            attachSql = String.format(
                    "ATTACH '%s' AS ducklake_catalog (DATA_PATH '%s');",
                    connectionString,
                    dataPath
            );
        }

        try {
            executeSql(conn, attachSql);
            log.debug("Attached PostgreSQL DuckLake catalog (schema: public, DATA_PATH: {}, READ_ONLY: {})",
                    dataPath, readOnly);
        } catch (SQLException e) {
            // Handle "already attached" error (shouldn't happen after check, but safety net)
            if (e.getMessage().contains("already attached")) {
                log.debug("PostgreSQL catalog already attached, skipping");
                return;
            }
            throw e;
        }
    }

    /**
     * Ensure catalog schema exists in PostgreSQL.
     * Creates the schema if it doesn't exist by temporarily connecting to PostgreSQL.
     *
     * This should be called once at application startup by DuckLakeCatalogInitializer.
     *
     * Note: DuckLake requires 'public' schema for its metadata tables. This method
     * creates the 'public' schema to ensure DuckLake can initialize properly.
     */
    public void ensureCatalogSchemaExists(Connection conn) throws SQLException {
        DatabaseCredentials creds = catalogCredentials;
        log.debug("Ensuring catalog schema 'public' exists in PostgreSQL (required by DuckLake)");

        // Install postgres_scanner if not already installed
        executeSql(conn, "INSTALL postgres_scanner;");
        executeSql(conn, "LOAD postgres_scanner;");

        // Check if already attached (in case of connection pooling)
        boolean wasAlreadyAttached = isDatabaseAttached(conn, "catalog_temp");

        if (!wasAlreadyAttached) {
            // Attach catalog PostgreSQL temporarily to create schema
            String tempAttach = String.format(
                    "ATTACH 'dbname=%s user=%s password=%s host=%s port=%s' AS catalog_temp (TYPE POSTGRES);",
                    creds.dbName,
                    creds.username,
                    creds.password,
                    creds.host,
                    creds.port
            );
            executeSql(conn, tempAttach);
            log.debug("Attached temporary PostgreSQL connection as 'catalog_temp'");
        }

        try {
            // Start explicit transaction to ensure schema creation is committed
            executeSql(conn, "BEGIN TRANSACTION;");

            // DuckLake requires 'public' schema for metadata tables - ensure it exists
            executeSql(conn, "CREATE SCHEMA IF NOT EXISTS catalog_temp.public;");
            log.debug("Created/verified 'public' schema (required by DuckLake)");

            // IMPORTANT: Commit the schema creation to PostgreSQL before detaching
            // This ensures the schema persists in PostgreSQL
            executeSql(conn, "COMMIT;");
            log.debug("Catalog schema created and committed");
        } catch (SQLException e) {
            // Rollback on error
            try {
                executeSql(conn, "ROLLBACK;");
            } catch (SQLException rollbackEx) {
                log.warn("Failed to rollback transaction: {}", rollbackEx.getMessage());
            }
            throw e;
        } finally {
            // Only detach if we attached it (avoid detaching pooled connection's attachment)
            if (!wasAlreadyAttached) {
                try {
                    // Force rollback any remaining transaction before detaching
                    try {
                        executeSql(conn, "ROLLBACK;");
                    } catch (SQLException e) {
                        // Ignore if no active transaction
                    }

                    // Detach catalog_temp
                    executeSql(conn, "DETACH catalog_temp;");

                    // Verify it's actually detached
                    if (isDatabaseAttached(conn, "catalog_temp")) {
                        throw new SQLException("CRITICAL: catalog_temp still attached after DETACH!");
                    }

                    log.debug("Successfully detached temporary PostgreSQL connection");
                } catch (SQLException e) {
                    log.error("CRITICAL: Failed to cleanup catalog_temp attachment: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to cleanup temporary PostgreSQL connection", e);
                }
            }
        }
    }

    /**
     * Configure DuckLake catalog compression settings.
     *
     * Sets global compression options for all Parquet files created by DuckLake.
     * Should be called once after catalog initialization.
     *
     * Configuration is applied using DuckLake's set_option() API:
     * - parquet_compression: Codec (zstd, snappy, gzip, etc.)
     * - parquet_compression_level: Compression intensity (ZSTD only)
     * - parquet_row_group_size: Rows per row group
     *
     * These settings affect all Parquet files written by DuckLake going forward.
     * Existing files are not recompressed.
     *
     * @param conn DuckDB connection (must be prepared for DuckLake with catalog attached)
     * @throws SQLException if configuration fails
     */
    public void configureDuckLakeCatalogSettings(Connection conn) throws SQLException {
        String codec = properties.getDucklake().getExport().getCodec();
        int compressionLevel = properties.getDucklake().getExport().getCompressionLevel();
        int rowGroupSize = properties.getDucklake().getExport().getRowGroupSize();

        log.debug("Configuring DuckLake catalog compression: codec={}, level={}, rowGroupSize={}",
                codec, compressionLevel, rowGroupSize);

        // Set global compression codec (lowercase required by DuckLake)
        String setCodecSql = String.format(
                "CALL ducklake_catalog.set_option('parquet_compression', '%s');",
                codec.toLowerCase()
        );
        executeSql(conn, setCodecSql);
        log.debug("Set DuckLake parquet_compression={}", codec.toLowerCase());

        // Set compression level (only applicable for ZSTD)
        if ("ZSTD".equalsIgnoreCase(codec)) {
            String setLevelSql = String.format(
                    "CALL ducklake_catalog.set_option('parquet_compression_level', %d);",
                    compressionLevel
            );
            executeSql(conn, setLevelSql);
            log.debug("Set DuckLake parquet_compression_level={}", compressionLevel);
        }

        // Set row group size if specified (skip if -1 = use default)
        if (rowGroupSize > 0) {
            String setRowGroupSql = String.format(
                    "CALL ducklake_catalog.set_option('parquet_row_group_size', %d);",
                    rowGroupSize
            );
            executeSql(conn, setRowGroupSql);
            log.debug("Set DuckLake parquet_row_group_size={}", rowGroupSize);
        } else {
            log.debug("Using DuckLake default row group size (~122,880 rows)");
        }

        log.info("✅ Configured DuckLake catalog compression: codec={}, level={}, rowGroupSize={}",
                codec, compressionLevel, rowGroupSize > 0 ? rowGroupSize : "default");
    }

    // ========== Private Helper Methods ==========

    /**
     * Attach DuckDB file-based DuckLake catalog.
     *
     * @param readOnly If true, attaches catalog in READ_ONLY mode
     */
    private void attachDuckDbCatalog(Connection conn, String dataPath, boolean readOnly) throws SQLException {
        // Check if already attached on this connection (connection pooling scenario)
        if (isDatabaseAttached(conn, "ducklake_catalog")) {
            log.debug("DuckDB DuckLake catalog already attached as 'ducklake_catalog', skipping");
            return;
        }

        String catalogPath = properties.getDucklake().getCatalogPath();

        // Ensure catalog directory exists
        Path catalogDir = Paths.get(catalogPath).getParent();
        if (catalogDir != null && !Files.exists(catalogDir)) {
            try {
                Files.createDirectories(catalogDir);
            } catch (IOException e) {
                throw new SQLException("Failed to create catalog directory: " + e.getMessage(), e);
            }
        }

        // Build ATTACH statement with optional READ_ONLY
        String attachSql;
        if (readOnly) {
            attachSql = String.format(
                    "ATTACH 'ducklake:%s' AS ducklake_catalog (READ_ONLY, DATA_PATH '%s');",
                    catalogPath,
                    dataPath
            );
        } else {
            attachSql = String.format(
                    "ATTACH 'ducklake:%s' AS ducklake_catalog (DATA_PATH '%s');",
                    catalogPath,
                    dataPath
            );
        }

        try {
            executeSql(conn, attachSql);
            log.debug("Attached DuckDB DuckLake catalog: {} (DATA_PATH: {}, READ_ONLY: {})",
                    catalogPath, dataPath, readOnly);
        } catch (SQLException e) {
            if (e.getMessage().contains("already attached")) {
                log.debug("DuckDB catalog already attached (file: {}), skipping", catalogPath);
                return;
            }
            // "Unique file handle conflict" = another DuckDB instance holds the lock.
            // This connection does NOT have the catalog — do not silently return.
            throw e;
        }
    }
}
