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
     * @param memoryLimit e.g. {@code "4GB"}; DuckDB spills beyond this
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
