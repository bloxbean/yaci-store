package com.bloxbean.cardano.yaci.store.analytics.query.service;

/**
 * Security-critical SQL validation for ad-hoc DuckDB queries.
 *
 * <p>Used by both the REST controller ({@code ParquetAnalyticsController}) and MCP service
 * ({@code McpAnalyticsService}) to enforce read-only query constraints and block dangerous
 * functions before queries reach the DuckDB engine.</p>
 *
 * <p><b>Defense layers (in order of application):</b></p>
 * <ol>
 *   <li><b>Comment stripping</b> — removes {@code /* ... * /} and {@code --} comments
 *       to prevent blocklist bypass via comment injection (e.g., {@code READ_PAR/ ** /QUET})</li>
 *   <li><b>Statement type check</b> — only {@code SELECT} and {@code WITH} allowed as top-level statements</li>
 *   <li><b>Semicolon ban</b> — prevents multi-statement injection</li>
 *   <li><b>Keyword blocklist</b> — blocks dangerous functions, DDL, DML, metadata access,
 *       and resource-exhaustion patterns via case-insensitive substring matching</li>
 * </ol>
 *
 * <p><b>Important security notes:</b></p>
 * <ul>
 *   <li>This validator is the <b>primary</b> defense against SQL injection. DuckDB's
 *       {@code enable_external_access=false} provides engine-level backup after view creation,
 *       but this validator runs first and must be comprehensive.</li>
 *   <li>The blocklist approach is inherently fragile — new DuckDB functions in future versions
 *       may not be covered. Periodic review against DuckDB release notes is recommended.</li>
 *   <li>All callers MUST {@code trim()} the SQL before passing it to {@link #validate(String)}.</li>
 * </ul>
 *
 * @see com.bloxbean.cardano.yaci.store.analytics.query.controller.ParquetAnalyticsController
 */
public final class SqlValidator {

    private SqlValidator() {
    }

    /** Maximum allowed query length in characters. Queries beyond this are rejected. */
    private static final int MAX_QUERY_LENGTH = 10_000;

    /**
     * Blocked keywords and function name prefixes.
     *
     * <p>Matched as case-insensitive substrings against the comment-stripped, uppercased SQL.
     * Entries ending with {@code _} act as prefix matches (e.g., {@code DUCKDB_} blocks
     * {@code duckdb_databases}, {@code duckdb_views}, etc.).</p>
     */
    private static final String[] BLOCKED_KEYWORDS = {
            // --- File I/O functions (DuckDB can read arbitrary files) ---
            "READ_CSV", "READ_JSON", "READ_PARQUET", "READ_TEXT", "READ_BLOB",
            "READ_NDJSON",
            "GLOB", "COPY", "EXPORT",

            // --- Extension management ---
            "INSTALL", "LOAD", "ATTACH", "DETACH",

            // --- Network access ---
            "HTTPFS", "HTTP_GET", "HTTP_POST",

            // --- System access ---
            "SYSTEM", "SHELL", "GETENV",

            // --- PostgreSQL direct access (must use unified views, not pg_live) ---
            "PG_", "POSTGRES_QUERY",

            // --- DuckDB internal metadata (credential/config leak prevention) ---
            // Blocks ALL duckdb_ prefixed functions: duckdb_databases(), duckdb_views(),
            // duckdb_tables(), duckdb_columns(), duckdb_settings(), duckdb_secrets(),
            // duckdb_extensions(), duckdb_functions(), duckdb_types(), etc.
            "DUCKDB_",

            // --- SQL information schema (leaks view definitions with file paths/credentials) ---
            "INFORMATION_SCHEMA",

            // --- DuckDB configuration ---
            "PRAGMA",
            "CURRENT_SETTING",

            // --- DDL/DML (defense-in-depth, also blocked by SELECT/WITH prefix check) ---
            "CREATE ", "ALTER ", "DROP ", "INSERT ", "UPDATE ", "DELETE ", "TRUNCATE ",
            "CHECKPOINT", "VACUUM",

            // --- Configuration changes ---
            // Space after SET prevents false positives on OFFSET/RESULTSET etc.
            // but catches SET variable = value patterns
            "SET ",

            // --- Resource exhaustion prevention ---
            "GENERATE_SERIES", "RANGE(",
            "RECURSIVE",
    };

    /**
     * Validate that a SQL query is a safe, read-only {@code SELECT}/{@code WITH} statement.
     *
     * <p>Strips SQL comments first to prevent blocklist bypass via comment injection,
     * then applies statement type, semicolon, and keyword checks.</p>
     *
     * @param sql the trimmed SQL query to validate
     * @throws IllegalArgumentException if the query fails any validation check
     */
    public static void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL query must not be empty");
        }

        if (sql.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "SQL query exceeds maximum length of " + MAX_QUERY_LENGTH + " characters");
        }

        // Step 1: Strip comments to prevent blocklist bypass
        // e.g., READ_PAR/**/QUET('/etc/passwd') → READ_PARQUET('/etc/passwd')
        String stripped = stripComments(sql);
        String upper = stripped.toUpperCase();

        // Step 2: Statement type — only SELECT or WITH allowed
        if (!upper.startsWith("SELECT") && !upper.startsWith("WITH")) {
            throw new IllegalArgumentException("Only SELECT/WITH statements are allowed");
        }

        // Step 3: No semicolons — prevents multi-statement injection
        if (stripped.contains(";")) {
            throw new IllegalArgumentException("Multiple statements (semicolons) are not allowed");
        }

        // Step 4: Keyword blocklist — checked against comment-stripped SQL
        for (String blocked : BLOCKED_KEYWORDS) {
            if (upper.contains(blocked)) {
                throw new IllegalArgumentException(
                        "Blocked keyword '" + blocked.trim() + "' is not allowed in ad-hoc queries");
            }
        }

        // Step 5: Block DuckDB replacement scans — direct file path references in FROM clause.
        // DuckDB auto-detects file extensions (e.g., SELECT * FROM '/tmp/data.parquet')
        // and internally calls read_parquet/read_csv without any function name in the SQL.
        // The keyword blocklist cannot catch this, so we block path-like patterns in string literals.
        rejectFilePathLiterals(stripped);
    }

    /**
     * Reject queries containing file-path-like string literals that could trigger
     * DuckDB's replacement scan (automatic file reading from {@code FROM} clause).
     *
     * <p>Blocks strings matching common path patterns:</p>
     * <ul>
     *   <li>Absolute paths: {@code '/etc/...'}, {@code '/tmp/...'}, {@code 'C:\...'}</li>
     *   <li>Relative paths with extensions: {@code './data.parquet'}, {@code '../secret.csv'}</li>
     *   <li>URL schemes: {@code 'http://...'}, {@code 'https://...'}, {@code 's3://...'}</li>
     * </ul>
     *
     * <p>Legitimate analytics queries never reference raw file paths — they use
     * table/view names (e.g., {@code block}, {@code transaction}).</p>
     *
     * @param sql the comment-stripped SQL
     * @throws IllegalArgumentException if a file-path-like literal is detected
     */
    private static void rejectFilePathLiterals(String sql) {
        int i = 0;
        int len = sql.length();

        while (i < len) {
            // Find next string literal
            if (sql.charAt(i) == '\'') {
                int start = i + 1;
                i++;
                // Find end of string literal (handle escaped quotes)
                while (i < len) {
                    if (sql.charAt(i) == '\'' && i + 1 < len && sql.charAt(i + 1) == '\'') {
                        i += 2; // skip escaped quote
                    } else if (sql.charAt(i) == '\'') {
                        break;
                    } else {
                        i++;
                    }
                }
                String literal = sql.substring(start, Math.min(i, len)).trim().toLowerCase();

                // Check for path-like patterns
                if (literal.startsWith("/")          // Unix absolute path
                        || literal.startsWith("./")  // Relative path
                        || literal.startsWith("../") // Parent directory
                        || literal.startsWith("~")   // Home directory
                        || (literal.length() >= 3 && literal.charAt(1) == ':' && (literal.charAt(2) == '\\' || literal.charAt(2) == '/')) // Windows path C:\
                        || literal.startsWith("http://")
                        || literal.startsWith("https://")
                        || literal.startsWith("s3://")
                        || literal.startsWith("s3a://")
                        || literal.startsWith("gs://")
                        || literal.startsWith("az://")
                        || literal.startsWith("abfss://")
                        || literal.startsWith("file://")) {
                    throw new IllegalArgumentException(
                            "File path or URL references are not allowed in queries");
                }
            }
            i++;
        }
    }

    /**
     * Strip SQL comments to prevent blocklist bypass via comment injection.
     *
     * <p>Handles two comment styles:</p>
     * <ul>
     *   <li>Block comments: {@code /* ... * /} (including nested)</li>
     *   <li>Line comments: {@code -- ...} (to end of line)</li>
     * </ul>
     *
     * <p>String literals ({@code '...'}) are preserved — comments inside strings
     * are not stripped, preventing false removal of legitimate data values.</p>
     *
     * @param sql the raw SQL string
     * @return SQL with all comments removed
     */
    static String stripComments(String sql) {
        StringBuilder result = new StringBuilder(sql.length());
        int i = 0;
        int len = sql.length();

        while (i < len) {
            char c = sql.charAt(i);

            // String literal — preserve everything inside quotes (including comment-like sequences)
            if (c == '\'') {
                result.append(c);
                i++;
                while (i < len) {
                    char sc = sql.charAt(i);
                    result.append(sc);
                    if (sc == '\'' && i + 1 < len && sql.charAt(i + 1) == '\'') {
                        // Escaped quote ('') — consume both
                        result.append('\'');
                        i += 2;
                    } else if (sc == '\'') {
                        i++;
                        break;
                    } else {
                        i++;
                    }
                }
                continue;
            }

            // Block comment /* ... */ — skip entirely (handles nesting)
            if (c == '/' && i + 1 < len && sql.charAt(i + 1) == '*') {
                i += 2;
                int depth = 1;
                while (i < len && depth > 0) {
                    if (sql.charAt(i) == '/' && i + 1 < len && sql.charAt(i + 1) == '*') {
                        depth++;
                        i += 2;
                    } else if (sql.charAt(i) == '*' && i + 1 < len && sql.charAt(i + 1) == '/') {
                        depth--;
                        i += 2;
                    } else {
                        i++;
                    }
                }
                // Replace comment with a space to preserve token boundaries
                result.append(' ');
                continue;
            }

            // Line comment -- ... — skip to end of line
            if (c == '-' && i + 1 < len && sql.charAt(i + 1) == '-') {
                i += 2;
                while (i < len && sql.charAt(i) != '\n') {
                    i++;
                }
                result.append(' ');
                continue;
            }

            // Normal character — keep
            result.append(c);
            i++;
        }

        return result.toString();
    }
}
