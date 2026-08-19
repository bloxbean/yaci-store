package com.bloxbean.cardano.yaci.store.analytics.query.executor;

import com.bloxbean.cardano.yaci.store.analytics.query.connection.ParquetReadConnectionProvider;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.lang.reflect.Array;
import java.util.Base64;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Executes SQL queries against DuckDB analytics views (Parquet + optional live PostgreSQL).
 *
 * <p>Provides typed query methods that handle connection lifecycle, timeouts,
 * result mapping, and result size limits. All connections are obtained via
 * {@link ParquetReadConnectionProvider} which uses DuckDB's {@code duplicate()}
 * pattern with semaphore-based concurrency control.</p>
 *
 * <p><b>Security measures:</b></p>
 * <ul>
 *   <li><b>Query timeout</b> — per-statement timeout (default 30s) cancels long-running queries;
 *       the engine uses plain (non-streaming) JDBC result sets so the timeout covers the whole
 *       execution</li>
 *   <li><b>Result row limit</b> — every statement is wrapped in a {@code LIMIT} (default 100 rows for
 *       ad-hoc SQL, hard cap {@code max-rows}, at most {@value #MAX_RESULT_ROWS}), so DuckDB stops
 *       producing rows at the cap and the JVM never holds more than {@code limit + 1} rows</li>
 *   <li><b>Semaphore concurrency</b> — limits concurrent queries to CPU core count</li>
 *   <li><b>Error sanitization</b> — internal DuckDB error details are not exposed to callers</li>
 * </ul>
 *
 * <p><b>Note:</b> This executor does NOT perform SQL validation. Callers MUST validate
 * queries via {@link com.bloxbean.cardano.yaci.store.analytics.query.service.SqlValidator}
 * before passing them to any query method.</p>
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.query", name = "enabled", havingValue = "true")
public class AnalyticsQueryExecutor {

    /**
     * Built-in ceiling for rows returned by any query. The configured
     * {@code yaci.store.analytics.query.max-rows} may lower the effective hard limit but not
     * raise it above this value. Prevents JVM OOM from queries like
     * {@code SELECT * FROM address_utxo} (1.47B rows).
     */
    public static final int MAX_RESULT_ROWS = 10_000;

    private final ParquetReadConnectionProvider connectionProvider;
    private final int hardRowLimit;
    private final int defaultRowLimit;

    public AnalyticsQueryExecutor(ParquetReadConnectionProvider connectionProvider,
                                  AnalyticsStoreProperties properties) {
        this.connectionProvider = connectionProvider;
        int configuredMax = properties.getQuery().getMaxRows();
        this.hardRowLimit = configuredMax > 0 ? Math.min(configuredMax, MAX_RESULT_ROWS) : MAX_RESULT_ROWS;
        int configuredDefault = properties.getQuery().getDefaultMaxRows();
        this.defaultRowLimit = configuredDefault > 0 ? Math.min(configuredDefault, hardRowLimit) : hardRowLimit;
        log.info("Analytics query row limits: default={} rows, hard limit={} rows (configured max-rows={}, ceiling={})",
                defaultRowLimit, hardRowLimit, configuredMax, MAX_RESULT_ROWS);
    }

    /** Result of {@link #execute(String, Integer)}: the rows, the limit that was applied and whether more existed. */
    public record QueryResult(List<Map<String, Object>> rows, int rowLimit, boolean truncated) {
    }

    /** Effective hard upper bound for rows per query (configured {@code max-rows}, at most {@link #MAX_RESULT_ROWS}). */
    public int getHardRowLimit() {
        return hardRowLimit;
    }

    /** Rows returned when a caller does not ask for a specific {@code maxRows}. */
    public int getDefaultRowLimit() {
        return defaultRowLimit;
    }

    /**
     * Resolve a caller-requested row limit: {@code null}/non-positive → the default limit,
     * anything larger than the hard limit → the hard limit.
     */
    private int resolveRowLimit(Integer requested) {
        if (requested == null || requested <= 0) {
            return defaultRowLimit;
        }
        return Math.min(requested, hardRowLimit);
    }

    /**
     * Execute a read-only query with a row limit, the way SQL front-ends cap results: the
     * statement is wrapped as {@code SELECT * FROM (<sql>) AS q LIMIT <limit + 1>} so DuckDB
     * stops producing rows at the cap (an inner {@code ORDER BY}/{@code LIMIT} keeps its
     * meaning). One extra row is requested only to detect truncation.
     *
     * <p>Note that DuckDB de-duplicates column labels of the wrapped statement: a query that
     * yields two columns named {@code hash} returns them as {@code hash} and {@code hash_1}.</p>
     *
     * @param sql       a single SELECT/WITH statement (must be pre-validated via {@code SqlValidator})
     * @param maxRows   requested row limit; {@code null} or non-positive → default, capped by the hard limit
     * @return rows (at most the resolved limit), the limit applied and whether more rows existed
     */
    public QueryResult execute(String sql, Integer maxRows) {
        int limit = resolveRowLimit(maxRows);
        List<Map<String, Object>> rows = new ArrayList<>();
        boolean truncated = fetch(sql, limit, this::mapRow, rows);
        return new QueryResult(rows, limit, truncated);
    }

    /**
     * Execute a query and map each row using the provided mapper function.
     *
     * <p>Results are capped at the hard row limit ({@link #getHardRowLimit()}); the query is
     * wrapped in a {@code LIMIT} like {@link #execute(String, Integer)}. Truncation is logged
     * but not reported to the caller — use {@link #execute(String, Integer)} (with
     * {@link #getHardRowLimit()} as {@code maxRows}) when the caller needs to know.</p>
     *
     * @param sql       the SQL query to execute (must be pre-validated via {@code SqlValidator})
     * @param rowMapper maps a {@link ResultSet} (positioned at a row) to a result object
     * @param <T>       the result type
     * @return list of mapped results, truncated to the hard row limit
     * @throws QueryExecutionException if the query fails or times out
     */
    public <T> List<T> query(String sql, Function<ResultSet, T> rowMapper) {
        List<T> results = new ArrayList<>();
        fetch(sql, hardRowLimit, rowMapper, results);
        return results;
    }

    /**
     * Execute a query and return results as a list of maps (column name to value), capped at
     * the hard row limit. Used by the pre-built endpoints; callers that must report truncation
     * (ad-hoc SQL, MCP tools) should use {@link #execute(String, Integer)} instead.
     *
     * @param sql the SQL query to execute (must be pre-validated via {@code SqlValidator})
     * @return list of row maps, each mapping column label to its value
     */
    public List<Map<String, Object>> queryForList(String sql) {
        return query(sql, this::mapRow);
    }

    /**
     * Wrap the statement in a LIMIT. The closing parenthesis goes on its own line so that a
     * trailing {@code -- comment} in the user's SQL cannot swallow it.
     */
    static String withRowLimit(String sql, int limit) {
        return "SELECT * FROM (\n" + sql + "\n) AS q LIMIT " + (limit + 1);
    }

    /**
     * Run the wrapped statement, append up to {@code limit} mapped rows to {@code sink}
     * and report whether an extra row (i.e. truncation) was seen.
     */
    private <T> boolean fetch(String sql, int limit, Function<ResultSet, T> rowMapper, List<T> sink) {
        long start = System.currentTimeMillis();
        String limited = withRowLimit(sql, limit);
        try (Connection conn = connectionProvider.getReadConnection();
             Statement stmt = conn.createStatement()) {

            stmt.setQueryTimeout(connectionProvider.getQueryTimeoutSeconds());

            try (ResultSet rs = stmt.executeQuery(limited)) {
                int fetched = 0;
                boolean truncated = false;
                while (rs.next()) {
                    if (fetched >= limit) {
                        truncated = true;   // the +1 row: more data exists
                        break;
                    }
                    sink.add(rowMapper.apply(rs));
                    fetched++;
                }

                long elapsed = System.currentTimeMillis() - start;
                if (truncated && limit >= hardRowLimit) {
                    // Hitting the hard cap is unusual for the pre-built endpoints and worth noticing
                    log.warn("Query result truncated at the hard limit of {} rows ({}ms): {}",
                            limit, elapsed, preview(sql));
                } else if (truncated) {
                    log.debug("Query result truncated at {} rows ({}ms): {}", limit, elapsed, preview(sql));
                } else {
                    log.debug("Query completed in {}ms, {} rows: {}", elapsed, fetched, preview(sql));
                }
                return truncated;
            }
        } catch (SQLException e) {
            long elapsed = System.currentTimeMillis() - start;
            // Log the (redacted) DuckDB reason for operators; callers only get a generic message.
            log.error("Analytics query failed after {}ms: {}",
                    elapsed, DuckDbConnectionHelper.redactSecrets(e.getMessage()));
            // Sanitize error message — do not expose internal DuckDB state, file paths,
            // or PostgreSQL connection details to the caller
            throw new QueryExecutionException("Query execution failed. Check query syntax and filters.");
        }
    }

    /**
     * Short single-line rendering of user SQL for log messages: whitespace (including line
     * breaks, which would otherwise let a caller inject log lines) is collapsed and the text
     * is cut at 100 characters.
     */
    static String preview(String sql) {
        String flat = sql.replaceAll("\\s+", " ").trim();
        return flat.length() > 100 ? flat.substring(0, 100) + "..." : flat;
    }

    private Map<String, Object> mapRow(ResultSet rs) {
        try {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            Map<String, Object> row = new LinkedHashMap<>(colCount);
            for (int i = 1; i <= colCount; i++) {
                row.put(meta.getColumnLabel(i), normalizeJdbcValue(rs.getObject(i)));
            }
            return row;
        } catch (SQLException e) {
            log.error("Failed to read analytics query result row: {}",
                    DuckDbConnectionHelper.redactSecrets(e.getMessage()));
            throw new QueryExecutionException("Failed to read query result row");
        }
    }

    static Object normalizeJdbcValue(Object value) throws SQLException {
        if (value == null) return null;
        if (value instanceof java.sql.Array sqlArray) {
            try {
                return normalizeArray(sqlArray.getArray());
            } finally {
                sqlArray.free();
            }
        }
        if (value instanceof Blob blob) {
            try {
                return Base64.getEncoder().encodeToString(blob.getBytes(1, Math.toIntExact(blob.length())));
            } finally {
                blob.free();
            }
        }
        if (value instanceof Clob clob) {
            try {
                return clob.getSubString(1, Math.toIntExact(clob.length()));
            } finally {
                clob.free();
            }
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        return value;
    }

    private static List<Object> normalizeArray(Object array) throws SQLException {
        int length = Array.getLength(array);
        List<Object> values = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            Object item = Array.get(array, i);
            values.add(item != null && item.getClass().isArray()
                    ? normalizeArray(item)
                    : normalizeJdbcValue(item));
        }
        return values;
    }

    /**
     * Execute a query that returns a single scalar value.
     *
     * @param sql  the SQL query to execute (must be pre-validated via {@code SqlValidator})
     * @param type the expected return type
     * @param <T>  the return type
     * @return the scalar value, or {@code null} if no rows returned
     */
    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Class<T> type) {
        List<Map<String, Object>> results = queryForList(sql);
        if (results.isEmpty()) {
            return null;
        }
        Object value = results.get(0).values().iterator().next();
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        // Handle numeric conversions
        if (value instanceof Number number) {
            if (type == Long.class || type == long.class) return (T) Long.valueOf(number.longValue());
            if (type == Integer.class || type == int.class) return (T) Integer.valueOf(number.intValue());
            if (type == Double.class || type == double.class) return (T) Double.valueOf(number.doubleValue());
        }
        return (T) value;
    }

    /**
     * A query failed inside DuckDB (syntax error, unknown table/column, timeout, ...). The
     * message is intentionally generic — DuckDB's own message may reveal file paths or
     * connection details — and callers should treat it as a client-side query problem.
     */
    public static class QueryExecutionException extends RuntimeException {
        public QueryExecutionException(String message) {
            super(message);
        }
    }
}
