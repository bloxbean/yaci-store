package com.bloxbean.cardano.yaci.store.snapshot.ducklake;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.function.Function;

/** Small helpers around an in-process DuckDB connection. */
public final class DuckDb {

    private DuckDb() {
    }

    /**
     * @param memoryLimit e.g. {@code "4GB"}; spillable operators can use disk, but some aggregates cannot
     * @param tempDir     per-worker spill directory, so parallel workers never share scratch space
     */
    public static Connection open(String memoryLimit, Path tempDir, int threads) throws SQLException {
        Properties props = new Properties();
        Connection conn = DriverManager.getConnection("jdbc:duckdb:", props);
        try (Statement st = conn.createStatement()) {
            if (memoryLimit != null) {
                st.execute("SET memory_limit='" + memoryLimit.replace("'", "") + "'");
            }
            if (tempDir != null) {
                st.execute("SET temp_directory='" + tempDir.toAbsolutePath().toString().replace("'", "''") + "'");
            }
            if (threads > 0) {
                st.execute("SET threads=" + threads);
            }
            st.execute("SET preserve_insertion_order=false");
        }
        return conn;
    }

    /** Memory each DuckDB thread should have at least; more threads than this would starve every one. */
    static final long BYTES_PER_THREAD = 512L * 1024 * 1024;

    /**
     * Threads for a connection with the given memory limit: one per {@link #BYTES_PER_THREAD}, at least
     * one and at most the available processors. DuckDB defaults to one thread per core, which on a
     * large host leaves each thread too little memory and fails queries a smaller host would finish.
     */
    public static int threadsFor(String memoryLimit) {
        long perThread = parseBytes(memoryLimit) / BYTES_PER_THREAD;
        return (int) Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), perThread));
    }

    /** Parses a DuckDB-style size such as {@code 1GB}, {@code 512MB} or {@code 4GiB}. */
    static long parseBytes(String value) {
        var m = java.util.regex.Pattern.compile("(?i)\\s*(\\d+(?:\\.\\d+)?)\\s*([KMGT]?I?B)?\\s*")
                .matcher(value == null ? "" : value);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid memory limit: " + value);
        }
        String unit = m.group(2) == null ? "B" : m.group(2).toUpperCase(java.util.Locale.ROOT);
        long base = unit.contains("I") ? 1024 : 1000;
        int power = switch (unit.charAt(0)) {
            case 'K' -> 1;
            case 'M' -> 2;
            case 'G' -> 3;
            case 'T' -> 4;
            default -> 0;
        };
        return (long) (Double.parseDouble(m.group(1)) * Math.pow(base, power));
    }

    /**
     * A scan of exported Parquet files that reads each file's own columns.
     *
     * <p>DuckLake lays files out in hive-style {@code epoch=N} and {@code date=D} directories and also
     * writes the partition column into every file. By default {@code read_parquet} detects those
     * directories and replaces the file's column with the directory value, typed BIGINT or DATE, so
     * the type the importer plans against depends on where a file happens to sit. Every snapshot scan
     * goes through here so that never happens.
     *
     * <p>Columns are matched by name across files. A column an exporter gained in a later release is
     * missing from the files written before it, and DuckLake keeps those files as they are; a scan
     * that took its columns from the first file would drop the new column without a word, or fail on
     * the older files. Matched by name, the older files read it as NULL.
     *
     * @param files a DuckDB list literal of file paths
     */
    public static String readParquet(String files) {
        return "read_parquet(" + files + ", hive_partitioning = false, union_by_name = true)";
    }

    public static void exec(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public static <T> T query(Connection conn, String sql, Function<ResultSet, T> mapper) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return mapper.apply(rs);
        }
    }

    public static long queryLong(Connection conn, String sql, long fallback) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                long v = rs.getLong(1);
                return rs.wasNull() ? fallback : v;
            }
            return fallback;
        }
    }

    public static String version(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT version()")) {
            return rs.next() ? rs.getString(1) : "unknown";
        }
    }
}
