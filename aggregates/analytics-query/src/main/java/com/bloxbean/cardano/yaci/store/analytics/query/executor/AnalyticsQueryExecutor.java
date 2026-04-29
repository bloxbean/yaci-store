package com.bloxbean.cardano.yaci.store.analytics.query.executor;

import com.bloxbean.cardano.yaci.store.analytics.query.connection.ParquetReadConnectionProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.sql.*;
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
 *   <li><b>Query timeout</b> — per-statement timeout (default 30s) prevents long-running queries</li>
 *   <li><b>Result row limit</b> — prevents OOM from unbounded result sets (default 10,000 rows)</li>
 *   <li><b>Semaphore concurrency</b> — limits concurrent queries to CPU core count</li>
 *   <li><b>Error sanitization</b> — internal DuckDB error details are not exposed to callers</li>
 * </ul>
 *
 * <p><b>Note:</b> This executor does NOT perform SQL validation. Callers MUST validate
 * queries via {@link com.bloxbean.cardano.yaci.store.analytics.query.service.SqlValidator}
 * before passing them to any query method.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.query", name = "enabled", havingValue = "true")
public class AnalyticsQueryExecutor {

    /**
     * Maximum number of rows returned from any query. Prevents JVM OOM from
     * queries like {@code SELECT * FROM address_utxo} (1.47B rows).
     * When exceeded, results are truncated and a warning is logged.
     */
    public static final int MAX_RESULT_ROWS = 10_000;

    private final ParquetReadConnectionProvider connectionProvider;

    /**
     * Execute a query and map each row using the provided mapper function.
     *
     * <p>Results are capped at {@link #MAX_RESULT_ROWS} to prevent JVM heap exhaustion.
     * When the limit is reached, iteration stops and remaining rows are discarded.</p>
     *
     * @param sql       the SQL query to execute (must be pre-validated via {@code SqlValidator})
     * @param rowMapper maps a {@link ResultSet} (positioned at a row) to a result object
     * @param <T>       the result type
     * @return list of mapped results, truncated to {@link #MAX_RESULT_ROWS}
     * @throws RuntimeException if the query fails or times out
     */
    public <T> List<T> query(String sql, Function<ResultSet, T> rowMapper) {
        long start = System.currentTimeMillis();
        try (Connection conn = connectionProvider.getReadConnection();
             Statement stmt = conn.createStatement()) {

            stmt.setQueryTimeout(connectionProvider.getQueryTimeoutSeconds());

            try (ResultSet rs = stmt.executeQuery(sql)) {
                List<T> results = new ArrayList<>();
                boolean truncated = false;

                while (rs.next()) {
                    if (results.size() >= MAX_RESULT_ROWS) {
                        truncated = true;
                        break;
                    }
                    results.add(rowMapper.apply(rs));
                }

                long elapsed = System.currentTimeMillis() - start;

                if (truncated) {
                    log.warn("Query result truncated at {} rows ({}ms): {}",
                            MAX_RESULT_ROWS, elapsed,
                            sql.length() > 100 ? sql.substring(0, 100) + "..." : sql);
                } else {
                    log.debug("Query completed in {}ms, {} rows: {}", elapsed, results.size(),
                            sql.length() > 100 ? sql.substring(0, 100) + "..." : sql);
                }
                return results;
            }
        } catch (SQLException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Query failed after {}ms: {} - {}", elapsed, sql, e.getMessage());
            // Sanitize error message — do not expose internal DuckDB state, file paths,
            // or PostgreSQL connection details to the caller
            throw new RuntimeException("Query execution failed. Check query syntax and filters.", e);
        }
    }

    /**
     * Execute a query and return results as a list of maps (column name to value).
     *
     * <p>Useful for dynamic/generic queries where the schema is not known at compile time.
     * Results are capped at {@link #MAX_RESULT_ROWS}.</p>
     *
     * @param sql the SQL query to execute (must be pre-validated via {@code SqlValidator})
     * @return list of row maps, each mapping column label to its value
     */
    public List<Map<String, Object>> queryForList(String sql) {
        return query(sql, rs -> {
            try {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                Map<String, Object> row = new LinkedHashMap<>(colCount);
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                return row;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to read result row", e);
            }
        });
    }

    /**
     * Check whether the last query executed by {@link #query} was truncated.
     * This is indicated by the result list size equaling {@link #MAX_RESULT_ROWS}.
     *
     * @param results the result list from a query method
     * @return {@code true} if the results were likely truncated
     */
    public static boolean isTruncated(List<?> results) {
        return results.size() >= MAX_RESULT_ROWS;
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
}
