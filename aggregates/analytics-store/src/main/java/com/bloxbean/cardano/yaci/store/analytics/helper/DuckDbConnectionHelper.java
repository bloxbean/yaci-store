package com.bloxbean.cardano.yaci.store.analytics.helper;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.config.DuckDbInstances;
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
import java.util.List;
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

    /** Catalog schema decision for a PostgreSQL catalog, made once per JVM. */
    private volatile CatalogSchemas.Resolution catalogResolution;
    /** Set once the catalog is known to exist (attached successfully). */
    private volatile boolean catalogKnownToExist;

    /**
     * Immutable credentials container.
     *
     * <p>{@code host}/{@code port} are libpq values (comma separated for several hosts).
     * {@code parameters} are the libpq connection parameters translated from the JDBC URL
     * (SSL settings, {@code target_session_attrs}, ...); {@code options} is the URL's
     * {@code options} value. {@code unavailableReason} is set when the URL is not a usable
     * PostgreSQL URL; the credentials can then not be used to attach PostgreSQL.</p>
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
        @Builder.Default
        java.util.Map<String, String> parameters = java.util.Map.of();
        String options;
        String unavailableReason;
    }

    public DuckDbConnectionHelper(Environment environment, AnalyticsStoreProperties properties) {
        this.environment = environment;
        this.properties = properties;

        // Resolve credentials at startup (cached for performance). A URL that is not a usable
        // PostgreSQL URL does not fail here: only attaching PostgreSQL needs it.
        this.sourceCredentials = resolveSourceCredentials();
        this.catalogCredentials = resolveCatalogCredentials();
        registerSecret(sourceCredentials.password);
        registerSecret(catalogCredentials.password);

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
     * Load the postgres_scanner extension, installing it only when it is not present yet.
     */
    public void installPostgresScanner(Connection conn) throws SQLException {
        loadExtension(conn, POSTGRES_EXTENSION, properties.getDuckdb().getExtensionDirectories());
    }

    /**
     * Load the ducklake extension, installing it only when it is not present yet.
     */
    public void installDuckLake(Connection conn) throws SQLException {
        loadExtension(conn, DUCKLAKE_EXTENSION, properties.getDuckdb().getExtensionDirectories());
    }

    public static final String DUCKLAKE_EXTENSION = "ducklake";
    public static final String POSTGRES_EXTENSION = "postgres_scanner";

    /**
     * Apply {@code yaci.store.analytics.duckdb.extension-directories}, if set, so that
     * {@code INSTALL}/{@code LOAD} use those directories. The setting is per DuckDB instance
     * and idempotent.
     */
    public void applyExtensionDirectories(Connection conn) throws SQLException {
        applyExtensionDirectories(conn, properties.getDuckdb().getExtensionDirectories());
    }

    private static void applyExtensionDirectories(Connection conn, List<String> directories) throws SQLException {
        String sql = extensionDirectoriesSql(directories);
        if (sql != null) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                // A locked-down instance (lock_configuration) was given the setting when it
                // was opened (DuckDbInstances.open); it cannot be changed any more.
                if (e.getMessage() == null || !e.getMessage().toLowerCase().contains("lock")) {
                    throw e;
                }
                log.debug("extension_directories not re-applied: configuration is locked");
            }
        }
    }

    /**
     * {@code SET extension_directories = [...]} for the given directories, or {@code null} when
     * none are configured.
     */
    public static String extensionDirectoriesSql(List<String> directories) {
        return DuckDbInstances.extensionDirectoriesSql(directories);
    }

    /**
     * Load a DuckDB extension without touching the network when it is already installed:
     * {@code LOAD} first, and only if that fails {@code INSTALL} + {@code LOAD}. When both fail
     * (offline host, extension repository outage), throw an error that names the extension, the
     * directories searched and how to pre-install it.
     */
    public static void loadExtension(Connection conn, String extension, List<String> extensionDirectories)
            throws SQLException {
        applyExtensionDirectories(conn, extensionDirectories);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("LOAD " + extension + ";");
            log.debug("Loaded DuckDB extension {}", extension);
            return;
        } catch (SQLException loadFailure) {
            log.debug("LOAD {} failed ({}); installing it", extension, loadFailure.getMessage());
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSTALL " + extension + ";");
            stmt.execute("LOAD " + extension + ";");
            log.info("Installed and loaded DuckDB extension {}", extension);
        } catch (SQLException e) {
            throw extensionUnavailable(conn, extension, e);
        }
    }

    private static SQLException extensionUnavailable(Connection conn, String extension, SQLException cause) {
        String version = querySetting(conn, "SELECT library_version FROM pragma_version()", "<duckdb-version>");
        String platform = querySetting(conn, "SELECT platform FROM pragma_platform()", "<platform>");
        String directories = querySetting(conn, "SELECT current_setting('extension_directories')::VARCHAR", "");
        if (directories.isBlank() || "[]".equals(directories)) {
            String single = querySetting(conn, "SELECT current_setting('extension_directory')", "");
            directories = single.isBlank() ? Paths.get(System.getProperty("user.home"), ".duckdb", "extensions").toString()
                    : single;
        }
        String message = "DuckDB extension '" + extension + "' (DuckDB " + version + ", platform " + platform
                + ") is not installed in " + directories + " and could not be installed: " + cause.getMessage()
                + ". If this host has no access to the DuckDB extension repository (extensions.duckdb.org), "
                + "install the extension on a machine with access, using the same DuckDB version, e.g. "
                + "duckdb -c \"SET extension_directory='/path/to/extensions'; INSTALL " + extension + ";\", "
                + "copy /path/to/extensions/" + version + "/" + platform + "/" + extension + ".duckdb_extension "
                + "to the same relative path on this host, and point yaci.store.analytics.duckdb.extension-directories "
                + "at that directory.";
        SQLException e = new SQLException(redactSecrets(message), cause.getSQLState(), cause.getErrorCode());
        e.setStackTrace(cause.getStackTrace());
        return e;
    }

    private static String querySetting(Connection conn, String sql, String fallback) {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next() && rs.getString(1) != null) {
                return rs.getString(1);
            }
        } catch (SQLException ignored) {
            // diagnostics only
        }
        return fallback;
    }

    /**
     * Attach source PostgreSQL database with specified alias, for the export pipeline.
     *
     * <p>Uses {@code yaci.store.analytics.source-statement-timeout-seconds} (default 0 = no
     * timeout: export queries are long-running). For the analytics query layer, use
     * {@link #attachSourceDatabase(Connection, String, int)} instead.</p>
     *
     * @param conn      DuckDB connection
     * @param aliasName database alias (e.g., "postgres_db" for Parquet, "source_db" for DuckLake)
     * @throws SQLException if the ATTACH command fails
     */
    public void attachSourceDatabase(Connection conn, String aliasName) throws SQLException {
        attachSourceDatabase(conn, aliasName, Math.max(0, properties.getSourceStatementTimeoutSeconds()));
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
        DatabaseCredentials creds = requirePostgres(sourceCredentials, "spring.datasource.url",
                "attach the source database as '" + aliasName + "'");

        // Put both settings in libpq's startup options. postgres_scanner can open a pool
        // of libpq connections, so a SET executed on one connection is not sufficient.
        java.util.List<String> settings = new java.util.ArrayList<>();
        if (creds.schema != null && !creds.schema.isEmpty()) {
            settings.add("search_path=" + creds.schema);
        }
        if (statementTimeoutSeconds > 0) {
            settings.add("statement_timeout=" + (statementTimeoutSeconds * 1000L));
        }
        String connInfo = libpqConnInfo(creds, settings);

        String cmd = "ATTACH '" + escapeSqlLiteral(connInfo) + "' AS "
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

    /** {@code password=} / {@code sslpassword=} (any case), followed by the value. */
    private static final Pattern PASSWORD_KEY = Pattern.compile("(?i)(?:ssl)?password\\s*=\\s*");

    /**
     * A password value: libpq-quoted ({@code 'a b\'c'}), libpq-quoted inside an SQL literal
     * ({@code ''a b\''c''}), or bare up to the next whitespace.
     */
    private static final Pattern PASSWORD_VALUE = Pattern.compile(
            "''(?:\\\\''|\\\\\\\\|[^'])*''|'(?:\\\\.|[^'\\\\])*'|\\S+");

    /** Configured passwords (and their escaped spellings) that are redacted wherever they appear. */
    private static final java.util.Set<String> KNOWN_SECRETS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Passwords shorter than this are only redacted after {@code password=}. */
    private static final int MIN_KNOWN_SECRET_LENGTH = 4;

    /**
     * Remember a secret so that {@link #redactSecrets} removes it from any message, also in
     * its libpq- and SQL-escaped forms.
     */
    static void registerSecret(String secret) {
        if (secret == null || secret.length() < MIN_KNOWN_SECRET_LENGTH) {
            return;
        }
        String libpq = secret.replace("\\", "\\\\").replace("'", "\\'");
        KNOWN_SECRETS.add(secret);
        KNOWN_SECRETS.add(libpq);
        KNOWN_SECRETS.add(escapeSqlLiteral(libpq));
        KNOWN_SECRETS.add(escapeSqlLiteral(secret));
    }

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
        // Known passwords first: a configured password may contain spaces or quotes and appear
        // unquoted, which no pattern can delimit. Only text right after "password=" is compared,
        // so a password that happens to be a common word does not garble the rest of the message.
        java.util.List<String> secrets = new java.util.ArrayList<>(KNOWN_SECRETS);
        secrets.sort((a, b) -> Integer.compare(b.length(), a.length()));
        StringBuilder out = new StringBuilder();
        java.util.regex.Matcher key = PASSWORD_KEY.matcher(message);
        int last = 0;
        while (key.find()) {
            if (key.start() < last) {
                continue; // inside a value that was already redacted
            }
            int valueStart = key.end();
            int valueEnd = knownSecretEnd(message, valueStart, secrets);
            if (valueEnd < 0) {
                java.util.regex.Matcher value = PASSWORD_VALUE.matcher(message);
                value.region(valueStart, message.length());
                valueEnd = value.lookingAt() ? value.end() : valueStart;
            }
            out.append(message, last, valueStart).append("***");
            last = valueEnd;
        }
        out.append(message, last, message.length());
        return out.toString();
    }

    /** End of a known secret (with its optional quotes) starting at {@code start}, or -1. */
    private static int knownSecretEnd(String message, int start, java.util.List<String> secrets) {
        for (String quote : new String[]{"''", "'", ""}) {
            if (!message.startsWith(quote, start)) {
                continue;
            }
            int from = start + quote.length();
            for (String secret : secrets) {
                if (!message.startsWith(secret, from)) {
                    continue;
                }
                int end = from + secret.length();
                if (!quote.isEmpty()) {
                    if (message.startsWith(quote, end)) {
                        return end + quote.length();
                    }
                } else if (end == message.length() || Character.isWhitespace(message.charAt(end))
                        || message.charAt(end) == '"') {
                    return end;
                }
            }
        }
        return -1;
    }

    private static void appendConnInfo(StringBuilder target, String key, String value) {
        if (!target.isEmpty()) target.append(' ');
        target.append(key).append('=').append(quoteConnInfoValue(value));
    }

    /**
     * Build a libpq connection string ({@code key='value' ...}, every value quoted and escaped)
     * from parsed credentials. Null credentials are omitted (libpq then uses its defaults:
     * {@code PGUSER}/{@code PGPASSWORD}, {@code ~/.pgpass}, ...). {@code settings} are added as
     * {@code -c name=value} startup options after the URL's own {@code options}.
     */
    static String libpqConnInfo(DatabaseCredentials creds, java.util.List<String> settings) {
        StringBuilder connInfo = new StringBuilder();
        if (creds.dbName != null && !creds.dbName.isEmpty()) appendConnInfo(connInfo, "dbname", creds.dbName);
        if (creds.username != null) appendConnInfo(connInfo, "user", creds.username);
        if (creds.password != null) appendConnInfo(connInfo, "password", creds.password);
        if (creds.host != null) appendConnInfo(connInfo, "host", creds.host);
        if (creds.port != null) appendConnInfo(connInfo, "port", creds.port);
        for (java.util.Map.Entry<String, String> e : creds.parameters.entrySet()) {
            appendConnInfo(connInfo, e.getKey(), e.getValue());
        }
        StringBuilder options = new StringBuilder();
        if (creds.options != null && !creds.options.isBlank()) {
            options.append(creds.options.trim());
        }
        if (settings != null) {
            for (String setting : settings) {
                if (!options.isEmpty()) options.append(' ');
                options.append("-c").append(escapeOptionValue(setting));
            }
        }
        if (!options.isEmpty()) appendConnInfo(connInfo, "options", options.toString());
        return connInfo.toString();
    }

    /** libpq splits {@code options} at whitespace; a backslash escapes the next character. */
    private static String escapeOptionValue(String value) {
        return value.replace("\\", "\\\\").replace(" ", "\\ ");
    }

    /**
     * Credentials that can be used to connect to PostgreSQL, or a clear error naming the
     * property when the configured URL is not a usable PostgreSQL URL.
     */
    private static DatabaseCredentials requirePostgres(DatabaseCredentials creds, String property, String purpose)
            throws SQLException {
        if (creds.unavailableReason != null) {
            throw new SQLException("Cannot " + purpose + ": " + property + " ('" + redactSecrets(creds.url)
                    + "') is not usable: " + creds.unavailableReason
                    + ". The analytics store needs a PostgreSQL JDBC URL (jdbc:postgresql://host:port/db).");
        }
        return creds;
    }

    private static String quoteConnInfoValue(String value) {
        String safe = value == null ? "" : value;
        return "'" + safe.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    static String escapeSqlLiteral(String value) {
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
     * <p>An existing catalog is attached <em>without</em> {@code DATA_PATH}, so DuckLake uses the
     * path recorded in the catalog, and that path is then compared with {@code export-path}: the
     * same directory written differently ({@code ./data/analytics} vs {@code /app/data/analytics},
     * a symbolic link) is accepted, a different directory is refused with a clear message. A new
     * catalog is created with {@code DATA_PATH = export-path}.</p>
     *
     * @param conn DuckDB connection
     * @param readOnly If true, attaches catalog in READ_ONLY mode (prevents writes)
     */
    public void attachDuckLakeCatalog(Connection conn, boolean readOnly) throws SQLException {
        CatalogType catalogType = CatalogType.of(properties.getDucklake().getCatalogType());
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
        if (catalogType == CatalogType.POSTGRESQL) {
            attachPostgresCatalog(conn, dataPath, readOnly);
        } else {
            attachDuckDbCatalog(conn, dataPath, readOnly);
        }
    }

    /** The supported {@code yaci.store.analytics.ducklake.catalog-type} values. */
    public enum CatalogType {
        DUCKDB, POSTGRESQL;

        /**
         * Parse {@code catalog-type} ({@code duckdb} or {@code postgresql}, case-insensitive).
         *
         * @throws IllegalStateException for any other value
         */
        public static CatalogType of(String value) {
            String v = value == null ? "" : value.trim();
            if ("duckdb".equalsIgnoreCase(v)) {
                return DUCKDB;
            }
            if ("postgresql".equalsIgnoreCase(v)) {
                return POSTGRESQL;
            }
            throw new IllegalStateException("Unsupported yaci.store.analytics.ducklake.catalog-type '" + value
                    + "'. Use 'postgresql' (catalog tables in PostgreSQL) or 'duckdb' (catalog in the file "
                    + "yaci.store.analytics.ducklake.catalog-path).");
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
     *
     * @param dataPath {@code DATA_PATH} for a new catalog, or null for an existing one (DuckLake
     *                 then uses the path recorded in the catalog)
     */
    static String duckLakeAttachOptions(String dataPath, boolean readOnly) {
        java.util.List<String> options = new java.util.ArrayList<>();
        if (readOnly) {
            options.add("READ_ONLY");
        }
        if (dataPath != null) {
            options.add("DATA_PATH '" + escapeSqlLiteral(dataPath) + "'");
        }
        if (!readOnly) {
            options.add("AUTOMATIC_MIGRATION true");
        }
        return String.join(", ", options);
    }

    /**
     * After attaching an existing catalog without {@code DATA_PATH}: refuse it when its recorded
     * data path is a different directory than {@code export-path}.
     */
    private void verifyDataPath(Connection conn, String dataPath, String catalogDescription) throws SQLException {
        String stored = null;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT value FROM ducklake_options('ducklake_catalog') WHERE option_name = 'data_path'")) {
            if (rs.next()) {
                stored = rs.getString(1);
            }
        }
        if (stored == null || CatalogSchemas.sameDirectory(stored, dataPath)) {
            if (stored != null && !stripTrailingSlash(stored).equals(stripTrailingSlash(dataPath))) {
                log.info("DuckLake catalog records DATA_PATH '{}', yaci.store.analytics.export-path is '{}': same "
                        + "directory, using the recorded path", stored, dataPath);
            }
            return;
        }
        try {
            executeSql(conn, "DETACH ducklake_catalog;");
        } catch (SQLException ignored) {
            // the failure below is what matters
        }
        throw new SQLException("The DuckLake catalog " + catalogDescription + " belongs to the export directory '"
                + stored + "', but yaci.store.analytics.export-path is '" + dataPath + "' (a different directory). "
                + "If the export directory was moved, set export-path back to '" + stored + "' or move the "
                + "directory back. If the catalog belongs to another yaci-store instance, give this instance a "
                + "catalog of its own (yaci.store.analytics.ducklake.catalog-schema for a PostgreSQL catalog, "
                + "catalog-path for a DuckDB catalog).");
    }

    private static String stripTrailingSlash(String path) {
        String p = path.trim();
        while (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    // ========== URL Parsing Utilities ==========

    /**
     * Parse a JDBC URL into libpq connection values. {@code currentSchema} becomes the schema
     * (default {@code public}); {@code user}/{@code password} in the URL win over the separate
     * properties, as in PgJDBC; SSL and other connection parameters are translated to libpq
     * (see {@link PostgresJdbcUrl#libpqParameters}). A URL that is not a PostgreSQL URL (for
     * example an H2 URL in tests) gives credentials with {@code unavailableReason} set.
     */
    static DatabaseCredentials parseCredentials(String url, String username, String password, String property) {
        PostgresJdbcUrl parsed;
        try {
            parsed = PostgresJdbcUrl.parse(url);
        } catch (IllegalArgumentException e) {
            return DatabaseCredentials.builder()
                    .url(url == null ? "" : url)
                    .username(username)
                    .password(password)
                    .schema("public")
                    .unavailableReason(e.getMessage())
                    .build();
        }
        String urlUser = parsed.parameter("user");
        String urlPassword = parsed.parameter("password");
        String schema = parsed.parameter("currentSchema");
        java.util.List<String> ignored = new java.util.ArrayList<>();
        java.util.Map<String, String> libpq = parsed.libpqParameters(ignored);
        if (!ignored.isEmpty()) {
            log.warn("{}: JDBC URL parameter(s) {} have no libpq equivalent and are not used for DuckDB's "
                    + "PostgreSQL connections", property, ignored);
        }
        return DatabaseCredentials.builder()
                .url(url)
                .username(urlUser != null && !urlUser.isEmpty() ? urlUser : username)
                .password(urlPassword != null ? urlPassword : password)
                .schema(schema == null || schema.isBlank() ? "public" : schema.trim())
                .host(parsed.libpqHost())
                .port(parsed.libpqPort())
                .dbName(parsed.database())
                .parameters(libpq)
                .options(parsed.parameter("options"))
                .build();
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
        return parseCredentials(
                environment.getProperty("spring.datasource.url"),
                environment.getProperty("spring.datasource.username"),
                environment.getProperty("spring.datasource.password"),
                "spring.datasource.url");
    }

    /**
     * Resolve catalog database credentials (catalog-specific if provided, else main datasource).
     */
    private DatabaseCredentials resolveCatalogCredentials() {
        boolean custom = properties.getDucklake().getCatalogUrl() != null;
        String url = custom ?
                properties.getDucklake().getCatalogUrl() :
                environment.getProperty("spring.datasource.url");

        String username = properties.getDucklake().getCatalogUsername() != null ?
                properties.getDucklake().getCatalogUsername() :
                environment.getProperty("spring.datasource.username");

        String password = properties.getDucklake().getCatalogPassword() != null ?
                properties.getDucklake().getCatalogPassword() :
                environment.getProperty("spring.datasource.password");

        return parseCredentials(url, username, password,
                custom ? "yaci.store.analytics.ducklake.catalog-url" : "spring.datasource.url");
    }

    /** This instance's source identity recorded in the catalog ({@code <database>/<schema>}). */
    public String catalogOwnerIdentity() {
        return CatalogSchemas.ownerIdentity(sourceCredentials.dbName, sourceCredentials.schema);
    }

    /**
     * The schema of the PostgreSQL DuckLake catalog, deciding it on first use (see
     * {@link CatalogSchemas}). Uses a temporary {@code catalog_temp} attach on {@code conn}.
     */
    public String catalogSchema(Connection conn) throws SQLException {
        return resolveCatalogSchema(conn).schema();
    }

    private synchronized CatalogSchemas.Resolution resolveCatalogSchema(Connection conn) throws SQLException {
        if (catalogResolution != null) {
            return catalogResolution;
        }
        DatabaseCredentials creds = requirePostgres(catalogCredentials,
                properties.getDucklake().getCatalogUrl() != null
                        ? "yaci.store.analytics.ducklake.catalog-url" : "spring.datasource.url",
                "attach the PostgreSQL DuckLake catalog");
        installPostgresScanner(conn);

        boolean wasAlreadyAttached = isDatabaseAttached(conn, "catalog_temp");
        if (!wasAlreadyAttached) {
            String tempAttach = "ATTACH '" + escapeSqlLiteral(libpqConnInfo(creds, null))
                    + "' AS catalog_temp (TYPE POSTGRES);";
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
            String connectionSchema = firstSchema(creds.schema);
            CatalogSchemas.Resolution resolution;
            try {
                resolution = CatalogSchemas.resolve(properties.getDucklake().getCatalogSchema(), connectionSchema,
                        catalogOwnerIdentity(), properties.getExportPath(), new PostgresCatalogLookup(conn));
            } catch (IllegalStateException e) {
                throw new SQLException(e.getMessage(), e);
            }
            if (resolution.legacyPublic()) {
                log.info("DuckLake catalog: using the existing catalog in schema 'public' (created before "
                        + "yaci.store.analytics.ducklake.catalog-schema existed). Set catalog-schema=public to "
                        + "make this explicit.");
            } else {
                log.info("DuckLake catalog schema: '{}' ({})", resolution.schema(),
                        resolution.exists() ? "existing catalog" : "new catalog");
            }
            ensureSchemaExists(conn, resolution.schema());
            catalogResolution = resolution;
            return resolution;
        } catch (SQLException e) {
            throw sanitize(e);
        } finally {
            if (!wasAlreadyAttached) {
                try {
                    executeSql(conn, "DETACH catalog_temp;");
                } catch (SQLException e) {
                    log.error("Failed to detach catalog_temp: {}", redactSecrets(e.getMessage()));
                }
            }
        }
    }

    /** {@code currentSchema} may be a search path ({@code a,b}): the catalog goes into the first. */
    private static String firstSchema(String schema) {
        if (schema == null || schema.isBlank()) {
            return CatalogSchemas.PUBLIC;
        }
        String first = schema.split(",")[0].trim();
        if (first.length() > 1 && first.startsWith("\"") && first.endsWith("\"")) {
            return first.substring(1, first.length() - 1).replace("\"\"", "\"");
        }
        return first.isEmpty() ? CatalogSchemas.PUBLIC : first;
    }

    /** Looks at the catalog database through the {@code catalog_temp} attach. */
    private final class PostgresCatalogLookup implements CatalogSchemas.CatalogLookup {
        private final Connection conn;

        private PostgresCatalogLookup(Connection conn) {
            this.conn = conn;
        }

        @Override
        public boolean hasCatalog(String schema) throws SQLException {
            String sql = "SELECT to_regclass(" + pgLiteral(pgIdentifier(schema) + ".ducklake_metadata")
                    + ") IS NOT NULL";
            return "true".equalsIgnoreCase(postgresQuery(conn, sql));
        }

        @Override
        public String metadata(String schema, String key) throws SQLException {
            String sql = "SELECT value FROM " + pgIdentifier(schema) + ".ducklake_metadata WHERE key = "
                    + pgLiteral(key) + " LIMIT 1";
            return postgresQuery(conn, sql);
        }
    }

    private static String postgresQuery(Connection conn, String postgresSql) throws SQLException {
        String sql = "SELECT * FROM postgres_query('catalog_temp', '" + escapeSqlLiteral(postgresSql) + "')";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static String pgIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String pgLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    /**
     * Create the catalog schema when it does not exist. Checks first, so a user without
     * {@code CREATE} on the database can use a schema that was created for it.
     */
    private void ensureSchemaExists(Connection conn, String schema) throws SQLException {
        String exists = postgresQuery(conn,
                "SELECT count(*) FROM pg_namespace WHERE nspname = " + pgLiteral(schema));
        if (exists != null && !"0".equals(exists)) {
            return;
        }
        try {
            executeSql(conn, "CALL postgres_execute('catalog_temp', '"
                    + escapeSqlLiteral("CREATE SCHEMA IF NOT EXISTS " + pgIdentifier(schema)) + "');");
            log.info("Created PostgreSQL schema '{}' for the DuckLake catalog", schema);
        } catch (SQLException e) {
            throw new SQLException("The DuckLake catalog schema '" + schema + "' does not exist and could not be "
                    + "created (" + redactSecrets(e.getMessage()) + "). Create it, or grant CREATE on the database "
                    + "to the catalog user, or set yaci.store.analytics.ducklake.catalog-schema to an existing "
                    + "schema the catalog user can create tables in.", e.getSQLState(), e.getErrorCode());
        }
    }

    /**
     * Attach PostgreSQL-based DuckLake catalog.
     *
     * @param readOnly If true, attaches catalog in READ_ONLY mode
     */
    private void attachPostgresCatalog(Connection conn, String dataPath, boolean readOnly) throws SQLException {
        DatabaseCredentials creds = catalogCredentials;
        CatalogSchemas.Resolution resolution = resolveCatalogSchema(conn);
        String schema = resolution.schema();
        boolean exists = resolution.exists() || catalogKnownToExist;

        String connectionString = "ducklake:postgres:"
                + libpqConnInfo(creds, java.util.List.of("search_path=" + pgIdentifier(schema)));

        String attachSql = "ATTACH '" + escapeSqlLiteral(connectionString) + "' AS ducklake_catalog ("
                + duckLakeAttachOptions(exists ? null : dataPath, readOnly)
                + ", METADATA_SCHEMA '" + escapeSqlLiteral(schema) + "');";

        try {
            executeSql(conn, attachSql);
            log.debug("Attached PostgreSQL DuckLake catalog (schema: {}, DATA_PATH: {}, READ_ONLY: {})",
                    schema, exists ? "recorded" : dataPath, readOnly);
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
        if (exists) {
            verifyDataPath(conn, dataPath, "in PostgreSQL schema '" + schema + "'");
        }
        catalogKnownToExist = true;
        if (!readOnly && resolution.claimOwner()) {
            claimCatalog(conn, schema);
        }
    }

    /** Record this instance's source in a catalog that does not record its owner yet. */
    private synchronized void claimCatalog(Connection conn, String schema) {
        CatalogSchemas.Resolution resolution = catalogResolution;
        if (resolution == null || !resolution.claimOwner()) {
            return;
        }
        String table = "__ducklake_metadata_ducklake_catalog." + quoteIdentifier(schema) + ".ducklake_metadata";
        try {
            executeSql(conn, "INSERT INTO " + table + " (key, value) VALUES ('" + CatalogSchemas.OWNER_KEY + "', '"
                    + escapeSqlLiteral(catalogOwnerIdentity()) + "');");
            log.info("DuckLake catalog in schema '{}' recorded as belonging to source {}", schema,
                    catalogOwnerIdentity());
        } catch (SQLException e) {
            log.warn("Could not record the source of the DuckLake catalog in schema '{}' ({}); a second "
                    + "instance using the same catalog is not detected", schema, redactSecrets(e.getMessage()));
        }
        catalogResolution = new CatalogSchemas.Resolution(resolution.schema(), true, false,
                resolution.legacyPublic());
    }

    /**
     * Decide the PostgreSQL catalog schema and make sure it exists (see {@link CatalogSchemas}).
     * Called once at application startup by DuckLakeCatalogInitializer; the decision is cached.
     */
    public void ensureCatalogSchemaExists(Connection conn) throws SQLException {
        String schema = catalogSchema(conn);
        log.debug("DuckLake catalog schema '{}' is ready", schema);
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

    /**
     * Attach DuckDB file-based DuckLake catalog.
     *
     * <p>When another process holds the catalog file (another yaci-store, or a {@code duckdb}
     * session that opened it, even with {@code -readonly}), DuckDB refuses with "Could not set
     * lock on file". The attach is retried for up to
     * {@code yaci.store.analytics.ducklake.catalog-lock-wait-seconds} before failing with a
     * message that says what holds the lock and what to do.</p>
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

        boolean exists = catalogKnownToExist || isNonEmptyFile(Paths.get(catalogPath));
        String attachSql = "ATTACH 'ducklake:" + escapeSqlLiteral(catalogPath) + "' AS ducklake_catalog ("
                + duckLakeAttachOptions(exists ? null : dataPath, readOnly) + ");";

        long waitMillis = Math.max(0, properties.getDucklake().getCatalogLockWaitSeconds()) * 1000L;
        long deadline = System.currentTimeMillis() + waitMillis;
        boolean warned = false;
        while (true) {
            try {
                executeSql(conn, attachSql);
                log.debug("Attached DuckDB DuckLake catalog: {} (DATA_PATH: {}, READ_ONLY: {})",
                        catalogPath, exists ? "recorded" : dataPath, readOnly);
                break;
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
                if (isLockConflict(e)) {
                    if (System.currentTimeMillis() < deadline) {
                        if (!warned) {
                            log.warn("DuckLake catalog file '{}' is locked by another process; waiting up to {}s "
                                    + "for it to be released ({})", catalogPath, waitMillis / 1000, e.getMessage());
                            warned = true;
                        }
                        sleepQuietly(1000);
                        continue;
                    }
                    SQLException locked = new SQLException("The DuckLake catalog file '" + catalogPath
                            + "' is locked by another process" + (waitMillis > 0 ? " (waited " + waitMillis / 1000
                            + "s)" : "") + ". Only one process can open a DuckDB catalog file, even read-only: stop "
                            + "the other process (for example a duckdb CLI or another yaci-store instance using "
                            + "the same catalog-path) and restart. To inspect the data while yaci-store runs, read "
                            + "the Parquet files or use the analytics query layer. DuckDB: " + e.getMessage(),
                            e.getSQLState(), e.getErrorCode());
                    locked.setStackTrace(e.getStackTrace());
                    throw locked;
                }
                throw e;
            }
        }
        if (exists) {
            verifyDataPath(conn, dataPath, "'" + catalogPath + "'");
        }
        catalogKnownToExist = true;
    }

    /** DuckDB's cross-process lock error ("Could not set lock on file ... Conflicting lock is held"). */
    static boolean isLockConflict(SQLException e) {
        String message = e.getMessage();
        return message != null && message.contains("Could not set lock on file");
    }

    private static boolean isNonEmptyFile(Path path) {
        try {
            return Files.isRegularFile(path) && Files.size(path) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static void sleepQuietly(long millis) throws SQLException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for the DuckLake catalog lock", e);
        }
    }
}
