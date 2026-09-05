package com.bloxbean.cardano.yaci.store.analytics.query.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Security-critical SQL validation for ad-hoc DuckDB queries.
 *
 * <p>Used by both the REST controller ({@code AnalyticsQueryController}) and MCP service
 * ({@code McpAnalyticsService}) to enforce read-only query constraints and block dangerous
 * functions before queries reach the DuckDB engine.</p>
 *
 * <p><b>Defense layers (in order of application):</b></p>
 * <ol>
 *   <li><b>Comment stripping</b> — removes {@code /* ... * /} and {@code --} comments
 *       before tokens are inspected</li>
 *   <li><b>Statement type check</b> — only {@code SELECT} and {@code WITH} allowed as top-level statements</li>
 *   <li><b>Semicolon ban</b> — prevents multi-statement injection</li>
 *   <li><b>Keyword blocklist</b> — blocks dangerous functions, DDL, DML, metadata access
 *       ({@code SHOW}/{@code DESCRIBE}/{@code SUMMARIZE}, {@code duckdb_*}/{@code pragma_*}
 *       table functions, {@code information_schema}, {@code pg_*}) and resource-exhaustion
 *       patterns using SQL tokens rather than substring matching</li>
 *   <li><b>File reference check</b> — rejects file-shaped names and path/URL-like quoted
 *       values that could trigger DuckDB replacement scans</li>
 * </ol>
 *
 * <p><b>Important security notes:</b></p>
 * <ul>
 *   <li>The DuckDB external-access sandbox is the primary file/network security boundary.
 *       This validator is defense in depth and restricts the exposed SQL surface.</li>
 *   <li>Token matching avoids false positives such as {@code asset}, {@code offset},
 *       {@code payload}, and {@code global_data}. Double-quoted identifiers such as
 *       {@code "set"} or {@code "show"} (e.g. custom-exporter columns) are allowed, since a
 *       quoted name can never act as a keyword; quoted <em>function</em> and catalog names
 *       ({@code "read_csv"(...)}, {@code "pg_live"."public"."x"}) remain blocked.</li>
 * </ul>
 *
 * @see com.bloxbean.cardano.yaci.store.analytics.query.controller.AnalyticsQueryController
 */
public final class SqlValidator {

    private SqlValidator() {
    }

    /** Maximum allowed query length in characters. Queries beyond this are rejected. */
    private static final int MAX_QUERY_LENGTH = 10_000;

    /**
     * Blocked keywords. Besides DDL/DML and extension management this covers DuckDB's
     * metadata statements ({@code SHOW}, {@code DESCRIBE}, {@code SUMMARIZE}), which DuckDB
     * accepts in subquery position ({@code SELECT * FROM (SHOW ALL TABLES)}) and which
     * enumerate every attached database — including all schemas of the federated
     * PostgreSQL server — rather than only the analytics views.
     *
     * <p>{@code RECURSIVE} is deliberately allowed: recursive CTEs are a legitimate
     * read-only pattern and runaway recursion is bounded by the per-query timeout and
     * DuckDB's memory limit.</p>
     */
    private static final Set<String> BLOCKED_WORDS = Set.of(
            "COPY", "EXPORT", "INSTALL", "LOAD", "ATTACH", "DETACH",
            "HTTPFS", "SYSTEM", "SHELL", "GETENV",
            "PRAGMA", "CREATE", "ALTER", "DROP", "INSERT", "UPDATE", "DELETE",
            "TRUNCATE", "CHECKPOINT", "VACUUM", "SET",
            "SHOW", "DESCRIBE", "SUMMARIZE", "EXPLAIN", "CALL"
    );

    private static final Set<String> BLOCKED_FUNCTIONS = Set.of(
            "READ_CSV", "READ_CSV_AUTO", "SNIFF_CSV",
            "READ_JSON", "READ_JSON_AUTO", "READ_JSON_OBJECTS", "READ_JSON_OBJECTS_AUTO",
            "READ_NDJSON", "READ_NDJSON_AUTO", "READ_NDJSON_OBJECTS",
            "READ_PARQUET", "PARQUET_SCAN", "READ_TEXT", "READ_BLOB", "GLOB",
            "PARQUET_METADATA", "PARQUET_SCHEMA", "PARQUET_FILE_METADATA", "PARQUET_KV_METADATA",
            "HTTP_GET", "HTTP_POST", "POSTGRES_QUERY", "CURRENT_SETTING",
            "QUERY", "QUERY_TABLE", "GENERATE_SERIES", "RANGE",
            "JSON_SERIALIZE_SQL", "JSON_DESERIALIZE_SQL", "JSON_EXECUTE_SERIALIZED_SQL",
            "REPEAT"
    );

    /**
     * File suffixes for which DuckDB performs an implicit replacement scan on an unresolved
     * relation name. Quotes are removed before matching so {@code data.csv}, {@code "data.csv"},
     * {@code "data"."csv"}, and mixed forms are treated identically.
     */
    private static final Pattern FILE_NAME = Pattern.compile(
            "(?i)\\.\\s*(?:csv|tsv|parquet|parq|pq|json|jsonl|ndjson)"
                    + "(?:\\s*\\.\\s*(?:gz|zst|zstd|bz2|xz))?(?![a-z0-9_])"
    );

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

        // Dollar quoting is unnecessary for analytics queries and DuckDB accepts a wider set
        // of tag characters than Java's identifier helpers. Rejecting '$' outright avoids a
        // lexer-desynchronization class instead of trying to duplicate DuckDB's tag grammar.
        if (sql.indexOf('$') >= 0) {
            throw new IllegalArgumentException("Dollar quoting and parameters are not allowed");
        }

        // Deliberately reject every semicolon, including semicolons inside literals or
        // quoted identifiers. The executor runs the original SQL text and DuckDB accepts
        // multiple statements, so a conservative raw-text check cannot be desynchronized
        // from DuckDB's lexer by an E'...' escape or an unusual quoted identifier.
        if (sql.indexOf(';') >= 0) {
            throw new IllegalArgumentException("Multiple statements (semicolons) are not allowed");
        }

        // Step 1: Strip comments before tokenizing.
        String stripped = stripComments(sql);
        List<Token> tokens = tokenize(stripped);

        // Step 2: Statement type — only SELECT or WITH allowed
        if (tokens.isEmpty()
                || (!tokens.get(0).text().equals("SELECT") && !tokens.get(0).text().equals("WITH"))) {
            throw new IllegalArgumentException("Only SELECT/WITH statements are allowed");
        }

        // Step 3: Token-aware blocklist. Exact token/function matching prevents substring
        // false positives such as ASSET containing SET or GLOBAL containing GLOB.
        //
        // Double-quoted names are identifiers, never keywords: "set" or "show" cannot start a
        // SET/SHOW statement, so the keyword blocklist only applies to bare words. Quoted names
        // CAN still name a dangerous function ("read_csv"('x'), "getenv"('HOME')) or catalog
        // object ("pg_live"."public"."x"), so the function, prefix and catalog checks apply to
        // quoted and bare tokens alike.
        for (Token token : tokens) {
            String word = token.text();
            boolean invoked = isFunctionInvocation(stripped, token.end());
            boolean blockedKeyword = !token.quoted() && BLOCKED_WORDS.contains(word);
            // duckdb_*() / pragma_*() table functions and pg_* / postgres_* names expose the
            // catalog of every attached database (including the federated PostgreSQL server).
            boolean blockedPrefix = word.startsWith("DUCKDB_") || word.startsWith("PRAGMA_")
                    || word.startsWith("PG_") || word.startsWith("POSTGRES_");
            boolean blockedCatalog = word.equals("SQLITE_MASTER") || word.equals("INFORMATION_SCHEMA");
            boolean blockedFunction = invoked
                    && (BLOCKED_FUNCTIONS.contains(word) || (token.quoted() && BLOCKED_WORDS.contains(word)));
            // "DESC" is the DESCRIBE alias when it starts a (sub)statement, e.g.
            // SELECT * FROM (DESC block); as an ORDER BY modifier it never follows "(".
            boolean blockedDescribeAlias = !token.quoted() && word.equals("DESC")
                    && followsOpenParen(stripped, token.start());
            if (blockedKeyword || blockedPrefix || blockedCatalog
                    || blockedFunction || blockedDescribeAlias) {
                throw new IllegalArgumentException(
                        "Blocked SQL token '" + word + "' is not allowed in ad-hoc queries");
            }
        }

        // Step 4: Block DuckDB replacement scans. Keep this deliberately conservative and easy
        // to audit: file-shaped names and quoted paths are forbidden anywhere in ad-hoc SQL.
        rejectFileReferences(stripped);
    }

    /**
     * Reject queries containing file-path-like string literals that could trigger
     * DuckDB's replacement scan (automatic file reading from {@code FROM} clause).
     *
     * <p>Blocks strings matching common path patterns:</p>
     * <ul>
     *   <li>Absolute paths: {@code '/etc/...'}, {@code '/tmp/...'}, {@code 'C:\...'}</li>
     *   <li>Explicit relative paths/traversal: {@code './data.parquet'},
     *       {@code '../secret.csv'}, {@code 'data/../../secret.csv'}</li>
     *   <li>URL schemes: {@code 'http://...'}, {@code 'https://...'}, {@code 's3://...'}</li>
     * </ul>
     *
     * <p>Legitimate analytics queries never reference raw file paths — they use
     * table/view names (e.g., {@code block}, {@code transaction}).</p>
     *
     * @param sql the comment-stripped SQL
     * @throws IllegalArgumentException if a file-path-like literal is detected
     */
    private static void rejectFileReferences(String sql) {
        String withoutIdentifierQuotes = sql.replace("\"", "");
        if (FILE_NAME.matcher(withoutIdentifierQuotes).find()) {
            throw new IllegalArgumentException("File references are not allowed in queries");
        }

        int i = 0;
        int len = sql.length();

        while (i < len) {
            if (sql.charAt(i) == '\'') {
                int start = i + 1;
                i = skipSingleQuotedString(sql, i);
                String value = sql.substring(start, Math.max(start, i - 1));
                if (isPathLike(value)) {
                    throw new IllegalArgumentException(
                            "File path or URL references are not allowed in queries");
                }
                continue;
            }
            if (sql.charAt(i) == '"') {
                int start = i + 1;
                i = skipDoubleQuotedIdentifier(sql, i);
                String identifier = sql.substring(start, Math.max(start, i - 1));
                if (isPathLike(identifier)) {
                    throw new IllegalArgumentException(
                            "File path or URL references are not allowed in queries");
                }
                continue;
            }
            i++;
        }
    }

    private static boolean isPathLike(String value) {
        String plainValue = value.replace("/*", "").replace("*/", "").replace("\\'", "");
        return plainValue.indexOf('/') >= 0
                || plainValue.indexOf('\\') >= 0
                || plainValue.startsWith("~");
    }

    private static List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;

        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c == '\'') {
                i = skipSingleQuotedString(sql, i);
                continue;
            }
            if (c == '"') {
                StringBuilder identifier = new StringBuilder();
                int start = i;
                i++;
                while (i < sql.length()) {
                    char quoted = sql.charAt(i);
                    if (quoted == '"' && i + 1 < sql.length() && sql.charAt(i + 1) == '"') {
                        identifier.append('"');
                        i += 2;
                    } else if (quoted == '"') {
                        i++;
                        break;
                    } else {
                        identifier.append(quoted);
                        i++;
                    }
                }
                if (!identifier.isEmpty()) {
                    tokens.add(new Token(identifier.toString().toUpperCase(Locale.ROOT), start, i, true));
                }
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = i++;
                while (i < sql.length()) {
                    char identifierChar = sql.charAt(i);
                    if (!Character.isLetterOrDigit(identifierChar) && identifierChar != '_') {
                        break;
                    }
                    i++;
                }
                tokens.add(new Token(sql.substring(start, i).toUpperCase(Locale.ROOT), start, i, false));
                continue;
            }
            i++;
        }

        return tokens;
    }

    /** True when the closest non-whitespace character before {@code tokenStart} is {@code (}. */
    private static boolean followsOpenParen(String sql, int tokenStart) {
        int i = tokenStart - 1;
        while (i >= 0 && Character.isWhitespace(sql.charAt(i))) {
            i--;
        }
        return i >= 0 && sql.charAt(i) == '(';
    }

    private static int skipSingleQuotedString(String sql, int start) {
        boolean backslashEscapes = isEscapeStringPrefix(sql, start);
        int i = start + 1;
        while (i < sql.length()) {
            if (backslashEscapes && sql.charAt(i) == '\\' && i + 1 < sql.length()) {
                i += 2;
            } else if (sql.charAt(i) == '\'') {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    i += 2;
                } else {
                    return i + 1;
                }
            } else {
                i++;
            }
        }
        return i;
    }

    private static int skipDoubleQuotedIdentifier(String sql, int start) {
        int i = start + 1;
        while (i < sql.length()) {
            if (sql.charAt(i) == '"') {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '"') {
                    i += 2;
                } else {
                    return i + 1;
                }
            } else {
                i++;
            }
        }
        return i;
    }

    private static boolean isEscapeStringPrefix(String sql, int quoteIndex) {
        if (quoteIndex == 0 || (sql.charAt(quoteIndex - 1) != 'E' && sql.charAt(quoteIndex - 1) != 'e')) {
            return false;
        }
        return quoteIndex == 1
                || (!Character.isLetterOrDigit(sql.charAt(quoteIndex - 2))
                && sql.charAt(quoteIndex - 2) != '_');
    }

    private static boolean isFunctionInvocation(String sql, int tokenEnd) {
        int i = tokenEnd;
        while (i < sql.length() && Character.isWhitespace(sql.charAt(i))) {
            i++;
        }
        return i < sql.length() && sql.charAt(i) == '(';
    }

    /**
     * A bare word or double-quoted identifier from the comment-stripped SQL.
     *
     * @param text   upper-cased token text
     * @param start  index of the first character (the opening quote for quoted identifiers)
     * @param end    index just past the last character (past the closing quote)
     * @param quoted true if the token came from a double-quoted identifier
     */
    private record Token(String text, int start, int end, boolean quoted) {
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

            // Quoted identifier — preserve everything, including comment markers.
            if (c == '"') {
                result.append(c);
                i++;
                while (i < len) {
                    char qc = sql.charAt(i);
                    result.append(qc);
                    if (qc == '"' && i + 1 < len && sql.charAt(i + 1) == '"') {
                        result.append('"');
                        i += 2;
                    } else if (qc == '"') {
                        i++;
                        break;
                    } else {
                        i++;
                    }
                }
                continue;
            }

            // String literal — preserve everything inside quotes (including comment-like sequences)
            if (c == '\'') {
                boolean backslashEscapes = isEscapeStringPrefix(sql, i);
                result.append(c);
                i++;
                while (i < len) {
                    char sc = sql.charAt(i);
                    result.append(sc);
                    if (backslashEscapes && sc == '\\' && i + 1 < len) {
                        result.append(sql.charAt(i + 1));
                        i += 2;
                    } else if (sc == '\'' && i + 1 < len && sql.charAt(i + 1) == '\'') {
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
