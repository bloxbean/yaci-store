package com.bloxbean.cardano.yaci.store.analytics.query.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
 *   <li><b>Control characters</b> — rejected (except tab, LF, CR); DuckDB stops reading at NUL</li>
 *   <li><b>Comment stripping</b> — removes {@code /* ... * /} and {@code --} comments
 *       before tokens are inspected; a line comment ends at LF or CR, exactly as in DuckDB</li>
 *   <li><b>Statement type check</b> — only {@code SELECT} and {@code WITH} allowed as top-level statements</li>
 *   <li><b>Semicolon ban</b> — prevents multi-statement injection</li>
 *   <li><b>Keyword blocklist</b> — blocks dangerous functions, DDL, DML, metadata access
 *       ({@code SHOW}/{@code DESCRIBE}/{@code SUMMARIZE}, {@code duckdb_*}/{@code pragma_*}
 *       table functions, {@code information_schema}, {@code pg_*}) and resource-exhaustion
 *       patterns using SQL tokens rather than substring matching</li>
 *   <li><b>Allocation bounds</b> — {@code lpad}/{@code rpad} widths and {@code printf}/{@code format}
 *       widths must be small literals; {@code repeat}/{@code list_resize}/{@code array_resize} are
 *       blocked. Their output is allocated outside DuckDB's {@code memory_limit}.</li>
 *   <li><b>Table functions</b> — default deny: only {@link #ALLOWED_TABLE_FUNCTIONS} may be called
 *       in table position; every other table function (file readers, metadata listings,
 *       {@code enable_logging}, {@code force_checkpoint}, ...) is rejected</li>
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
            "JSON_SERIALIZE_PLAN",
            // Unbounded single-call allocations: the output size comes straight from an
            // argument and is allocated outside DuckDB's memory_limit accounting.
            "REPEAT", "LIST_RESIZE", "ARRAY_RESIZE",
            // Instance-wide state changes, file writes and log access. Read connections are
            // duplicate() handles of ONE DuckDB instance, so e.g. enable_logging(storage='file')
            // would record every other caller's SQL into a file inside the sandbox, and
            // lock_configuration does not stop these table functions.
            "ENABLE_LOGGING", "DISABLE_LOGGING", "TRUNCATE_DUCKDB_LOGS", "WRITE_LOG",
            "ENABLE_PROFILING", "DISABLE_PROFILING",
            "ENABLE_PEG_PARSER", "DISABLE_PEG_PARSER", "CHECK_PEG_PARSER", "SQL_AUTO_COMPLETE",
            "FORCE_CHECKPOINT", "SLEEP_MS",
            // The remaining DuckDB 1.5.5 table functions and table macros. Table functions are
            // already default-denied in FROM position (see ALLOWED_TABLE_FUNCTIONS); listing them
            // here as well rejects them by name wherever they appear.
            "ARROW_SCAN", "ARROW_SCAN_DUMB", "ICU_CALENDAR_NAMES", "PARQUET_BLOOM_PROBE",
            "PARQUET_FULL_METADATA", "READ_DUCKDB", "READ_POSTGRES_BINARY", "REPEAT_ROW", "SEQ_SCAN",
            "SUMMARY", "TEST_ALL_TYPES", "TEST_VECTOR_TYPES", "WHICH_SECRET", "HISTOGRAM_VALUES"
    );

    /**
     * The only table functions ad-hoc SQL may call in table position ({@code FROM}, {@code JOIN},
     * {@code LATERAL}, a {@code FROM} list, {@code PIVOT}/{@code UNPIVOT}). Every other function
     * invoked there is rejected, including table functions added by future DuckDB versions or
     * extensions: table functions are where DuckDB keeps its file readers, catalog/metadata
     * listings and instance-wide switches ({@code enable_logging}, {@code force_checkpoint}, ...),
     * while the analytics views never need one. These three only expand their own argument.
     */
    static final Set<String> ALLOWED_TABLE_FUNCTIONS = Set.of("UNNEST", "JSON_EACH", "JSON_TREE");

    /** Words that introduce a table reference, so a call right after them is a table function. */
    private static final Set<String> TABLE_REF_INTRODUCERS = Set.of("FROM", "JOIN", "LATERAL", "PIVOT", "UNPIVOT");

    /** Keywords that can be followed by "(" without being a function call. */
    private static final Set<String> NON_FUNCTION_KEYWORDS = Set.of(
            "LATERAL", "SELECT", "FROM", "JOIN", "ON", "USING", "AS", "IN", "VALUES", "WHERE", "AND", "OR",
            "NOT", "EXISTS", "ANY", "ALL", "SOME", "OVER", "FILTER", "WITHIN", "WITH", "RECURSIVE",
            "MATERIALIZED", "PIVOT", "UNPIVOT", "UNION", "EXCEPT", "INTERSECT", "BY", "HAVING", "QUALIFY");

    /**
     * Keywords that end a {@code FROM} list at the same nesting level, so a following comma no
     * longer separates table references.
     */
    private static final Set<String> FROM_LIST_TERMINATORS = Set.of(
            "SELECT", "WHERE", "GROUP", "HAVING", "QUALIFY", "WINDOW", "ORDER", "LIMIT", "OFFSET",
            "UNION", "EXCEPT", "INTERSECT", "RETURNING");

    /**
     * Largest pad width accepted by {@code lpad}/{@code rpad}. Both allocate the padded value in
     * one step outside DuckDB's memory_limit accounting ({@code lpad('a', 1000000000, 'b')} grows
     * the process by ~3 GB even with {@code memory_limit=256MB}); their result is exactly that
     * wide, so a literal bound also bounds nested calls.
     */
    static final int MAX_PAD_WIDTH = 10_000;

    /**
     * Longest width/precision (in digits) accepted inside a {@code printf}/{@code format} spec:
     * {@code printf('%1000000000d', 1)} allocates like {@code lpad}. Three digits allow widths and
     * precisions up to 999, far beyond any column formatting.
     */
    private static final int MAX_FORMAT_SPEC_DIGITS = 3;

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

        // DuckDB stops reading the statement at a NUL character, so "SELECT * FROM t) AS q\0"
        // would drop the LIMIT wrapper the executor appends. No other control character
        // (except tab, line feed and carriage return) has a place in a query, and rejecting
        // them keeps the validator from ever seeing more or less text than DuckDB executes.
        rejectControlCharacters(sql);

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
            if (invoked) {
                checkAllocationBounds(stripped, token);
            }
        }

        // Step 3b: Default-deny table functions in table position.
        rejectTableFunctions(stripped);

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

    private static void rejectControlCharacters(String sql) {
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if ((c < 0x20 && c != '\t' && c != '\n' && c != '\r') || c == 0x7f) {
                throw new IllegalArgumentException("Control characters are not allowed in queries");
            }
        }
    }

    /**
     * Bound the functions whose output size is taken from an argument and allocated outside
     * DuckDB's memory_limit accounting (see {@link #MAX_PAD_WIDTH}).
     */
    private static void checkAllocationBounds(String sql, Token token) {
        String name = token.text();
        boolean pad = name.equals("LPAD") || name.equals("RPAD");
        boolean printf = name.equals("PRINTF");
        boolean format = name.equals("FORMAT");
        if (!pad && !printf && !format) {
            return;
        }
        int open = sql.indexOf('(', token.end());
        List<String> args = splitArguments(sql, open);
        boolean memberCall = precededByDot(sql, token.start());

        if (pad) {
            // lpad(s, n, c); the member form s.lpad(n, c) passes the string as the receiver.
            String width = args.size() == 3 ? args.get(1) : args.size() == 2 && memberCall ? args.get(0) : null;
            if (width == null || !isIntegerLiteralAtMost(width.strip(), MAX_PAD_WIDTH)) {
                throw new IllegalArgumentException(name.toLowerCase(Locale.ROOT)
                        + "() needs an integer literal width of at most " + MAX_PAD_WIDTH);
            }
            return;
        }

        // printf/format: the format string must be visible, i.e. a plain literal first argument
        // (the member form 'fmt'.format(...) hides it in the receiver).
        String fmt = args.isEmpty() || memberCall ? null : args.get(0).strip();
        String literal = fmt == null ? null : plainStringLiteral(fmt);
        if (literal == null) {
            throw new IllegalArgumentException(name.toLowerCase(Locale.ROOT)
                    + "() needs a string literal as its first argument");
        }
        boolean bounded = printf ? printfSpecsBounded(literal) : formatSpecsBounded(literal);
        if (!bounded) {
            throw new IllegalArgumentException(name.toLowerCase(Locale.ROOT)
                    + "() widths and precisions must be literal and at most "
                    + MAX_FORMAT_SPEC_DIGITS + " digits");
        }
    }

    /** printf specs: no '*' (width/precision from an argument) and no long digit runs. */
    private static boolean printfSpecsBounded(String fmt) {
        int i = 0;
        while (i < fmt.length()) {
            if (fmt.charAt(i) != '%') {
                i++;
                continue;
            }
            if (i + 1 < fmt.length() && fmt.charAt(i + 1) == '%') {
                i += 2;
                continue;
            }
            int end = i + 1;
            while (end < fmt.length() && !Character.isLetter(fmt.charAt(end))) {
                end++;
            }
            if (!specBounded(fmt.substring(i + 1, end), '*')) {
                return false;
            }
            i = end + 1;
        }
        return true;
    }

    /** fmt-style specs: no nested '{' (width/precision from an argument) and no long digit runs. */
    private static boolean formatSpecsBounded(String fmt) {
        int i = 0;
        while (i < fmt.length()) {
            if (fmt.charAt(i) != '{') {
                i++;
                continue;
            }
            if (i + 1 < fmt.length() && fmt.charAt(i + 1) == '{') {
                i += 2;
                continue;
            }
            int end = fmt.indexOf('}', i + 1);
            if (end < 0) {
                end = fmt.length();
            }
            if (!specBounded(fmt.substring(i + 1, end), '{')) {
                return false;
            }
            i = end + 1;
        }
        return true;
    }

    private static boolean specBounded(String spec, char dynamicMarker) {
        if (spec.indexOf(dynamicMarker) >= 0) {
            return false;
        }
        int run = 0;
        for (int i = 0; i < spec.length(); i++) {
            run = Character.isDigit(spec.charAt(i)) ? run + 1 : 0;
            if (run > MAX_FORMAT_SPEC_DIGITS) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIntegerLiteralAtMost(String text, int max) {
        if (text.isEmpty() || text.length() > 9) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) < '0' || text.charAt(i) > '9') {
                return false;
            }
        }
        return Integer.parseInt(text) <= max;
    }

    /** The value of a plain {@code '...'} literal ({@code ''} unescaped), or null if {@code text} is anything else. */
    private static String plainStringLiteral(String text) {
        if (text.length() < 2 || text.charAt(0) != '\'' || skipSingleQuotedString(text, 0) != text.length()
                || text.charAt(text.length() - 1) != '\'') {
            return null;
        }
        return text.substring(1, text.length() - 1).replace("''", "'");
    }

    private static boolean precededByDot(String sql, int tokenStart) {
        int i = tokenStart - 1;
        while (i >= 0 && Character.isWhitespace(sql.charAt(i))) {
            i--;
        }
        return i >= 0 && sql.charAt(i) == '.';
    }

    /**
     * Top-level arguments of the call whose "(" is at {@code open}, as raw text. Nested
     * parentheses/brackets/braces and quoted text are kept intact.
     */
    private static List<String> splitArguments(String sql, int open) {
        List<String> args = new ArrayList<>();
        int depth = 0;
        int argStart = open + 1;
        int i = open + 1;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c == '\'') {
                i = skipSingleQuotedString(sql, i);
                continue;
            }
            if (c == '"') {
                i = skipDoubleQuotedIdentifier(sql, i);
                continue;
            }
            if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                if (depth == 0) {
                    String last = sql.substring(argStart, i);
                    if (!args.isEmpty() || !last.isBlank()) {
                        args.add(last);
                    }
                    return args;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                args.add(sql.substring(argStart, i));
                argStart = i + 1;
            }
            i++;
        }
        args.add(sql.substring(argStart));
        return args;
    }

    private enum LexKind { WORD, QUOTED, OPEN, CLOSE, COMMA, DOT, OTHER }

    /** A significant lexeme of the comment-stripped SQL; {@code text} is upper-cased for words. */
    private record Lexeme(LexKind kind, String text) {
    }

    /** Per-nesting-level state for {@link #rejectTableFunctions(String)}. */
    private static final class Level {
        /** A level that holds a query (top level, a subquery, or any non-call parenthesis). */
        boolean query;
        /** The level was opened by the "(" of a function call (or a keyword like IN/EXISTS). */
        final boolean call;
        /** Inside the FROM clause of this level, so a comma separates table references. */
        boolean inFrom;
        boolean empty = true;

        Level(boolean query, boolean call) {
            this.query = query;
            this.call = call;
        }
    }

    /**
     * Reject every function called in table position unless it is in
     * {@link #ALLOWED_TABLE_FUNCTIONS}. DuckDB only accepts table functions as table references
     * (calling one as a scalar is a binder error), so this covers every table function and table
     * macro, including ones this class does not know about by name.
     */
    private static void rejectTableFunctions(String sql) {
        List<Lexeme> lexemes = lex(sql);
        Deque<Level> levels = new ArrayDeque<>();
        levels.push(new Level(true, false));

        for (int i = 0; i < lexemes.size(); i++) {
            Lexeme lx = lexemes.get(i);
            Level level = levels.peek();
            if (level.empty && lx.kind() != LexKind.CLOSE) {
                level.empty = false;
                // EXISTS (SELECT ...), IN (FROM ...), ARRAY(SELECT ...): a call-shaped
                // parenthesis that starts a query is a query level.
                if (level.call && lx.kind() == LexKind.WORD
                        && (lx.text().equals("SELECT") || lx.text().equals("FROM") || lx.text().equals("WITH"))) {
                    level.query = true;
                }
            }
            switch (lx.kind()) {
                case OPEN -> {
                    Lexeme prev = i > 0 ? lexemes.get(i - 1) : null;
                    boolean call = lx.text().equals("(") && prev != null
                            && (prev.kind() == LexKind.WORD || prev.kind() == LexKind.QUOTED);
                    // '(' that is not a call: subquery or grouping. '[' / '{': list or struct literal.
                    levels.push(new Level(!call && lx.text().equals("("), call));
                }
                case CLOSE -> {
                    if (levels.size() > 1) {
                        levels.pop();
                    }
                }
                case WORD -> {
                    String word = lx.text();
                    if (level.query && word.equals("FROM") && !isDistinctFrom(lexemes, i)) {
                        level.inFrom = true;
                    } else if (FROM_LIST_TERMINATORS.contains(word)) {
                        level.inFrom = false;
                    }
                    checkTableFunctionCall(lexemes, i, level);
                }
                case QUOTED -> checkTableFunctionCall(lexemes, i, level);
                default -> {
                    // commas, dots, literals and operators carry no state
                }
            }
        }
    }

    private static void checkTableFunctionCall(List<Lexeme> lexemes, int i, Level level) {
        Lexeme name = lexemes.get(i);
        boolean call = i + 1 < lexemes.size() && lexemes.get(i + 1).text().equals("(");
        if (!call || (name.kind() == LexKind.WORD && NON_FUNCTION_KEYWORDS.contains(name.text()))) {
            return;
        }
        // Skip a qualifier chain: catalog.schema.function(
        int j = i;
        while (j >= 2 && lexemes.get(j - 1).kind() == LexKind.DOT
                && (lexemes.get(j - 2).kind() == LexKind.WORD || lexemes.get(j - 2).kind() == LexKind.QUOTED)) {
            j -= 2;
        }
        Lexeme prev = j > 0 ? lexemes.get(j - 1) : null;
        boolean tablePosition = prev != null
                && ((prev.kind() == LexKind.WORD && TABLE_REF_INTRODUCERS.contains(prev.text())
                        && (!prev.text().equals("FROM") || (level.query && !isDistinctFrom(lexemes, j - 1))))
                || (prev.kind() == LexKind.COMMA && level.inFrom));
        if (tablePosition && !ALLOWED_TABLE_FUNCTIONS.contains(name.text())) {
            throw new IllegalArgumentException("Table function '" + name.text().toLowerCase(Locale.ROOT)
                    + "' is not allowed in ad-hoc queries");
        }
    }

    /** True for the FROM of {@code IS [NOT] DISTINCT FROM}. */
    private static boolean isDistinctFrom(List<Lexeme> lexemes, int fromIndex) {
        if (fromIndex < 2 || !lexemes.get(fromIndex - 1).text().equals("DISTINCT")) {
            return false;
        }
        String before = lexemes.get(fromIndex - 2).text();
        return before.equals("IS") || before.equals("NOT");
    }

    private static List<Lexeme> lex(String sql) {
        List<Lexeme> lexemes = new ArrayList<>();
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '\'') {
                i = skipSingleQuotedString(sql, i);
                lexemes.add(new Lexeme(LexKind.OTHER, "'"));
            } else if (c == '"') {
                int start = i;
                i = skipDoubleQuotedIdentifier(sql, i);
                String identifier = sql.substring(start + 1, Math.max(start + 1, i - 1)).replace("\"\"", "\"");
                lexemes.add(new Lexeme(LexKind.QUOTED, identifier.toUpperCase(Locale.ROOT)));
            } else if (Character.isLetter(c) || c == '_') {
                int start = i++;
                while (i < sql.length() && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) {
                    i++;
                }
                lexemes.add(new Lexeme(LexKind.WORD, sql.substring(start, i).toUpperCase(Locale.ROOT)));
            } else if (Character.isDigit(c)) {
                // Numbers, including 1e5 and 1.5, must not produce WORD or DOT lexemes.
                while (i < sql.length() && (Character.isLetterOrDigit(sql.charAt(i))
                        || sql.charAt(i) == '.' || sql.charAt(i) == '_')) {
                    i++;
                }
                lexemes.add(new Lexeme(LexKind.OTHER, "0"));
            } else {
                LexKind kind = switch (c) {
                    case '(', '[', '{' -> LexKind.OPEN;
                    case ')', ']', '}' -> LexKind.CLOSE;
                    case ',' -> LexKind.COMMA;
                    case '.' -> LexKind.DOT;
                    default -> LexKind.OTHER;
                };
                lexemes.add(new Lexeme(kind, String.valueOf(c)));
                i++;
            }
        }
        return lexemes;
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
     *   <li>Line comments: {@code -- ...} (to the next {@code \n} or {@code \r}, as in DuckDB's lexer)</li>
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
                // DuckDB's lexer ends a line comment at '\n' OR '\r'. Stopping only at '\n'
                // would hide everything after a bare '\r' from the checks while DuckDB runs it.
                while (i < len && sql.charAt(i) != '\n' && sql.charAt(i) != '\r') {
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
