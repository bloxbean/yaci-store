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
import java.util.regex.Pattern;

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
     * Attach source PostgreSQL database with specified alias (no statement timeout).
     *
     * <p>Used by the export pipeline where queries are long-running and timeout
     * is not desired. For the analytics query layer, use
     * {@link #attachSourceDatabase(Connection, String, int)} instead.</p>
     *
     * @param conn      DuckDB connection
     * @param aliasName database alias (e.g., "postgres_db" for Parquet, "source_db" for DuckLake)
     * @throws SQLException if the ATTACH command fails
     */
    public void attachSourceDatabase(Connection conn, String aliasName) throws SQLException {
        attachSourceDatabase(conn, aliasName, 0);
    }

    /**
     * Attach source PostgreSQL database with specified alias and optional statement timeout.
     *
     * <p>The attachment is always {@code READ_ONLY}, preventing any writes to PostgreSQL
     * through DuckDB queries. If a {@code statementTimeoutSeconds} greater than 0 is provided,
     * it is set as a PostgreSQL connection parameter via {@code -cstatement_timeout=<ms>},
     * ensuring that heavy analytical queries are cancelled by PostgreSQL after the timeout.</p>
     *
     * <p>Checks if the database is already attached (connection pooling aware).</p>
     *
     * @param conn                     DuckDB connection
     * @param aliasName                database alias (e.g., "pg_live" for analytics queries)
     * @param statementTimeoutSeconds  PostgreSQL statement timeout in seconds (0 = no timeout)
     * @throws SQLException if the ATTACH command fails
     */
    public void attachSourceDatabase(Connection conn, String aliasName, int statementTimeoutSeconds) throws SQLException {
        DatabaseCredentials creds = sourceCredentials;

        StringBuilder connInfo = new StringBuilder();
        appendConnInfo(connInfo, "dbname", creds.dbName);
        appendConnInfo(connInfo, "user", creds.username);
        appendConnInfo(connInfo, "password", creds.password);
        appendConnInfo(connInfo, "host", creds.host);
        appendConnInfo(connInfo, "port", creds.port);

        // Put both settings in libpq's startup options. postgres_scanner can open a pool
        // of libpq connections, so a SET executed on one connection is not sufficient.
        StringBuilder options = new StringBuilder();
        if (creds.schema != null && !creds.schema.isEmpty()) {
            options.append("-csearch_path=").append(creds.schema);
        }
        if (statementTimeoutSeconds > 0) {
            if (!options.isEmpty()) options.append(' ');
            options.append("-cstatement_timeout=").append(statementTimeoutSeconds * 1000L);
        }
        if (!options.isEmpty()) appendConnInfo(connInfo, "options", options.toString());

        String cmd = "ATTACH '" + escapeSqlLiteral(connInfo.toString()) + "' AS "
                + quoteIdentifier(aliasName) + " (TYPE POSTGRES, READ_ONLY);";

        if (isDatabaseAttached(conn, aliasName)) {
            log.debug("Source database '{}' already attached, skipping", aliasName);
            return;
        }
        try {
            executeSql(conn, cmd);
            log.debug("Attached source PostgreSQL database as '{}'", aliasName);
        } catch (SQLException e) {
            if (isAlreadyAttached(e)) {
                log.debug("Source database '{}' already attached, skipping", aliasName);
                return;
            }
            throw sanitizedAttachFailure(
                    "Failed to attach source PostgreSQL database as '" + aliasName + "'", e);
        }
    }

    /**
     * True only for DuckDB's "a database with this alias is already attached on this
     * connection" error ({@code database with name "x" already exists}).
     *
     * <p>Deliberately does NOT match {@code "already attached"}: DuckDB's cross-instance
     * error {@code Unique file handle conflict: Cannot attach ... - the database file ... is
     * already attached by database ...} contains that phrase too, but it means the file is
     * held by <em>another</em> DuckDB instance in this JVM and this connection does NOT have
     * the catalog. Treating it as "already attached" hides the real failure and the next
     * {@code USE ducklake_catalog} fails with a misleading "No catalog + schema named
     * ducklake_catalog found".</p>
     */
    private static boolean isAlreadyAttached(SQLException e) {
        String errorMessage = e.getMessage();
        return errorMessage != null && errorMessage.contains("already exists");
    }

    /**
     * True for DuckDB's in-process single-open violation ({@code Unique file handle conflict}):
     * the database file is open in another DuckDB instance of this JVM.
     */
    public static boolean isFileHandleConflict(SQLException e) {
        String errorMessage = e.getMessage();
        return errorMessage != null && errorMessage.contains("Unique file handle conflict");
    }

    /**
     * Wrap a DuckDB/postgres_scanner attach failure so that neither the message nor the
     * cause chain can leak the libpq connection string. DuckDB echoes the full connection
     * string (including {@code password=...}) in attach errors, and callers log
     * {@code e.getMessage()} and stack traces. The redacted libpq reason is kept so that
     * failures remain diagnosable.
     */
    private static SQLException sanitizedAttachFailure(String context, SQLException e) {
        return sanitize(context, e);
    }

    /**
     * Return a copy of {@code e} whose message has credentials redacted and whose cause
     * chain (which may carry the raw libpq connection string) is dropped. The original
     * stack trace, SQLState and vendor code are preserved so the failure stays diagnosable.
     *
     * <p>Use this before logging or re-throwing any DuckDB error that may originate from
     * {@code postgres_scanner} or a PostgreSQL-backed DuckLake catalog: connection failures
     * (including reconnects in the middle of a query) echo the full connection string.</p>
     */
    public static SQLException sanitize(SQLException e) {
        return sanitize(null, e);
    }

    private static SQLException sanitize(String context, SQLException e) {
        String reason = redactSecrets(e.getMessage());
        String message;
        if (context == null) {
            message = reason;
        } else if (reason == null || reason.isBlank()) {
            message = context;
        } else {
            message = context + ": " + reason;
        }
        SQLException sanitized = new SQLException(message, e.getSQLState(), e.getErrorCode());
        sanitized.setStackTrace(e.getStackTrace());
        return sanitized;
    }

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "(?i)(password\\s*=\\s*)('(?:\\\\.|[^'\\\\])*'|\\S+)");

    /**
     * Redact credential values from a DuckDB/libpq error message.
     *
     * <p>Replaces every {@code password=<value>} occurrence (quoted or bare, e.g. from an
     * echoed {@code dbname=... user=... password=... host=...} connection string) with
     * {@code password=***}. Safe to call with {@code null}.</p>
     */
    public static String redactSecrets(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        return PASSWORD_PATTERN.matcher(message).replaceAll("$1***");
    }

    private static void appendConnInfo(StringBuilder target, String key, String value) {
        if (!target.isEmpty()) target.append(' ');
        target.append(key).append('=').append(quoteConnInfoValue(value));
    }

    private static String quoteConnInfoValue(String value) {
        String safe = value == null ? "" : value;
        return "'" + safe.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
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
            log.debug("Error checking whether database '{}' is attached: {}",
                    databaseName, redactSecrets(e.getMessage()));
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

        // Use catalog database — retry once if stale catalog state is detected
        try {
            executeSql(conn, "USE ducklake_catalog;");
        } catch (SQLException e) {
            log.warn("USE ducklake_catalog failed ({}); re-attaching catalogs", redactSecrets(e.getMessage()));

            // Detach stale catalogs
            try { executeSql(conn, "DETACH IF EXISTS ducklake_catalog;"); } catch (SQLException ignored) {}
            if (needsSource) {
                try { executeSql(conn, "DETACH IF EXISTS source_db;"); } catch (SQLException ignored) {}
            }

            // Re-install extensions and re-attach
            installDuckLake(conn);
            attachDuckLakeCatalog(conn, readOnly);
            if (needsSource) {
                installPostgresScanner(conn);
                attachSourceDatabase(conn, "source_db");
            }

            // Retry — fail hard if this also fails
            executeSql(conn, "USE ducklake_catalog;");
            log.info("USE ducklake_catalog succeeded after re-attach");
        }
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

        // Connection-pool aware: the catalog stays attached on a pooled connection.
        if (isDatabaseAttached(conn, "ducklake_catalog")) {
            log.debug("DuckLake catalog already attached on this connection, skipping");
            return;
        }

        // Attach the appropriate catalog type
        if ("postgresql".equalsIgnoreCase(catalogType)) {
            attachPostgresCatalog(conn, dataPath, readOnly);
        } else {
            attachDuckDbCatalog(conn, dataPath, readOnly);
        }
    }

    /**
     * ATTACH options shared by both catalog types.
     *
     * <p>Read-write attaches (the export writer) pass {@code AUTOMATIC_MIGRATION true}: a
     * newer DuckLake extension refuses to open a catalog written in an older format
     * ("DuckLake catalog version mismatch ... set AUTOMATIC_MIGRATION to TRUE") unless it may
     * migrate the metadata in place. Migration is one-way — an older DuckDB/DuckLake can no
     * longer open the migrated catalog — so back up the catalog before upgrading DuckDB.
     * Read-only attaches cannot migrate and therefore do not request it.</p>
     */
    private static String duckLakeAttachOptions(String dataPath, boolean readOnly) {
        String data = "DATA_PATH '" + escapeSqlLiteral(dataPath) + "'";
        return readOnly ? "READ_ONLY, " + data : data + ", AUTOMATIC_MIGRATION true";
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
        String attachSql = String.format(
                "ATTACH '%s' AS ducklake_catalog (%s);",
                connectionString,
                duckLakeAttachOptions(dataPath, readOnly)
        );

        try {
            executeSql(conn, attachSql);
            log.debug("Attached PostgreSQL DuckLake catalog (schema: public, DATA_PATH: {}, READ_ONLY: {})",
                    dataPath, readOnly);
        } catch (SQLException e) {
            // Handle "already attached" error (shouldn't happen after check, but safety net)
            if (isAlreadyAttached(e)) {
                log.debug("PostgreSQL catalog already attached, skipping");
                return;
            }
            // The DuckLake/postgres_scanner error echoes the catalog connection string,
            // including the password. Callers log the message and stack trace.
            throw sanitizedAttachFailure("Failed to attach PostgreSQL DuckLake catalog", e);
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
            try {
                executeSql(conn, tempAttach);
            } catch (SQLException e) {
                // Attach errors echo the connection string including the password.
                throw sanitizedAttachFailure(
                        "Failed to attach PostgreSQL DuckLake catalog database as 'catalog_temp'", e);
            }
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
                    log.error("CRITICAL: Failed to cleanup catalog_temp attachment: {}",
                            redactSecrets(e.getMessage()));
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

        disableDuckLakeDataInlining(conn);
    }

    /**
     * Keep every committed row in a Parquet data file.
     *
     * <p>DuckLake &ge; 1.0 (DuckDB 1.5.x) inlines small inserts into the catalog database by
     * default ({@code data_inlining_row_limit}); those rows only reach a Parquet file when
     * flushed. Many epoch tables export a handful of rows per partition, so they would sit
     * in the catalog indefinitely. The analytics query layer reads the committed data files
     * directly (it cannot attach the catalog, see {@code DuckLakeCatalogSnapshotReader}), so
     * inlined rows would be invisible to it. Persist {@code data_inlining_row_limit = 0} in
     * the catalog and flush anything a previous run may have inlined. Both calls are best
     * effort: older DuckLake extensions without inlining simply reject them.</p>
     */
    private void disableDuckLakeDataInlining(Connection conn) {
        try {
            executeSql(conn, "CALL ducklake_catalog.set_option('data_inlining_row_limit', 0);");
            log.info("DuckLake data inlining disabled (data_inlining_row_limit=0)");
        } catch (SQLException e) {
            log.debug("DuckLake data_inlining_row_limit not supported by this DuckLake version ({}); "
                    + "nothing to disable", redactSecrets(e.getMessage()));
            return;
        }
        try {
            executeSql(conn, "CALL ducklake_flush_inlined_data('ducklake_catalog');");
            log.debug("Flushed previously inlined DuckLake data to Parquet");
        } catch (SQLException e) {
            log.warn("Could not flush inlined DuckLake data ({}); rows inlined by an earlier run stay "
                    + "invisible to the analytics query layer until flushed", redactSecrets(e.getMessage()));
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * Attach DuckDB file-based DuckLake catalog.
     *
     * @param readOnly If true, attaches catalog in READ_ONLY mode
     */
    private void attachDuckDbCatalog(Connection conn, String dataPath, boolean readOnly) throws SQLException {
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
        String attachSql = String.format(
                "ATTACH 'ducklake:%s' AS ducklake_catalog (%s);",
                catalogPath,
                duckLakeAttachOptions(dataPath, readOnly)
        );

        try {
            executeSql(conn, attachSql);
            log.debug("Attached DuckDB DuckLake catalog: {} (DATA_PATH: {}, READ_ONLY: {})",
                    catalogPath, dataPath, readOnly);
        } catch (SQLException e) {
            if (isAlreadyAttached(e)) {
                log.debug("DuckDB catalog already attached (file: {}), skipping", catalogPath);
                return;
            }
            if (isFileHandleConflict(e)) {
                // Another DuckDB instance in this JVM (normally the export writer's pooled
                // connection) has the catalog file open. DuckDB allows exactly one instance per
                // file per process, READ_ONLY included. This connection does NOT have the
                // catalog — do not silently return.
                SQLException conflict = new SQLException(
                        "DuckLake catalog file '" + catalogPath + "' is already open in another DuckDB "
                                + "instance of this JVM (DuckDB allows one instance per file per process, "
                                + "READ_ONLY included). Read the catalog through the export writer's "
                                + "connection instead of attaching it here. Cause: " + e.getMessage(),
                        e.getSQLState(), e.getErrorCode());
                conflict.setStackTrace(e.getStackTrace());
                throw conflict;
            }
            throw e;
        }
    }
}
