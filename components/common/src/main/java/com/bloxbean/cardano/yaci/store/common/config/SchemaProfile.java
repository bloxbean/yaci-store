package com.bloxbean.cardano.yaci.store.common.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

/** Target-local configuration; never restored from a producer's snapshot. */
public final class SchemaProfile {
    public static final String TABLE = "_yaci_store_schema_profile";
    private SchemaProfile() {}

    public static void write(Connection conn, String schema, Map<String, Boolean> modules) throws SQLException {
        if (schema == null) throw new SQLException("No target schema selected by the datasource");
        String table = qualified(conn, schema);
        conn.setAutoCommit(false);
        try (var st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS " + table + " (module varchar(100) PRIMARY KEY, enabled boolean NOT NULL)");
            st.execute("DELETE FROM " + table);
            try (var ps = conn.prepareStatement("INSERT INTO " + table + " VALUES (?,?)")) {
                for (var entry : modules.entrySet()) {
                    ps.setString(1, entry.getKey()); ps.setBoolean(2, entry.getValue()); ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback(); throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
    public static Map<String, Boolean> read(Connection conn, String schema) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT 1 FROM information_schema.tables WHERE table_schema=? AND table_name=?")) {
            ps.setString(1, schema); ps.setString(2, TABLE);
            try (var rs = ps.executeQuery()) { if (!rs.next()) return Map.of(); }
        }
        Map<String, Boolean> result = new TreeMap<>();
        try (var st = conn.createStatement(); var rs = st.executeQuery("SELECT module,enabled FROM " + qualified(conn, schema))) {
            while (rs.next()) result.put(rs.getString(1), rs.getBoolean(2));
        }
        return result;
    }
    private static String qualified(Connection conn, String schema) throws SQLException {
        String quote = conn.getMetaData().getIdentifierQuoteString().trim();
        if (quote.isEmpty()) throw new SQLException("Database does not support quoted identifiers");
        return quote + schema.replace(quote, quote + quote) + quote + "." + quote + TABLE + quote;
    }
}
