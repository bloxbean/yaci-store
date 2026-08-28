package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Reads the live PostgreSQL target schema. */
public class PgSchema {

    private final Connection conn;
    private final String schema;

    public PgSchema(Connection conn, String schema) {
        this.conn = conn;
        this.schema = Identifiers.requireSqlIdentifier(schema, "target schema");
    }

    public String schema() {
        return schema;
    }

    /** Base and partitioned tables only; views, indexes and partition children are excluded. */
    public List<String> baseTables() throws SQLException {
        List<String> out = new ArrayList<>();
        String sql = """
                SELECT c.relname
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = ?
                  AND c.relkind IN ('r','p')
                  AND NOT EXISTS (SELECT 1 FROM pg_inherits i WHERE i.inhrelid = c.oid)
                ORDER BY c.relname
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
        }
        return out;
    }

    public TargetTable table(String name) throws SQLException {
        Identifiers.requireSqlIdentifier(name, "target table");
        Map<String, String> cols = new LinkedHashMap<>();
        Map<String, Boolean> nullable = new LinkedHashMap<>();
        Map<String, String> defaults = new LinkedHashMap<>();
        boolean partitioned = false;

        String sql = """
                SELECT a.attname, format_type(a.atttypid, a.atttypmod), NOT a.attnotnull,
                       pg_get_expr(d.adbin, d.adrelid), c.relkind
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped
                LEFT JOIN pg_attrdef d ON d.adrelid = c.oid AND d.adnum = a.attnum
                WHERE n.nspname = ? AND c.relname = ?
                ORDER BY a.attnum
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String col = rs.getString(1);
                    cols.put(col, rs.getString(2));
                    nullable.put(col, rs.getBoolean(3));
                    String def = rs.getString(4);
                    if (def != null) {
                        defaults.put(col, def);
                    }
                    partitioned = "p".equals(rs.getString(5));
                }
            }
        }
        if (cols.isEmpty()) {
            throw new IllegalStateException("Target table " + schema + "." + name + " does not exist");
        }
        return new TargetTable(schema, name, cols, nullable, defaults, primaryKey(name), partitioned);
    }

    public List<String> primaryKey(String table) throws SQLException {
        List<String> out = new ArrayList<>();
        String sql = """
                SELECT a.attname
                FROM pg_index i
                JOIN pg_class c ON c.oid = i.indrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ANY(i.indkey)
                WHERE n.nspname = ? AND c.relname = ? AND i.indisprimary
                ORDER BY a.attnum
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
        }
        return out;
    }

    /**
     * Stable fingerprint of the whole schema shape. Import requires this to equal the value recorded
     * at export time, so a snapshot cannot be loaded into a schema it was not built for.
     */
    public String fingerprint() throws SQLException {
        StringBuilder sb = new StringBuilder();
        String sql = """
                SELECT c.relname, a.attname, format_type(a.atttypid, a.atttypmod), a.attnotnull
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped
                WHERE n.nspname = ?
                  AND c.relkind IN ('r','p')
                  AND c.relname <> 'flyway_schema_history'
                  AND NOT EXISTS (SELECT 1 FROM pg_inherits i WHERE i.inhrelid = c.oid)
                ORDER BY c.relname, a.attname
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sb.append(rs.getString(1)).append('.').append(rs.getString(2)).append(':')
                            .append(rs.getString(3)).append(':').append(rs.getBoolean(4)).append('\n');
                }
            }
        }
        return Digests.sha256Hex(sb.toString());
    }

    /** Fingerprint of the applied Flyway history, so release mismatch is detected explicitly. */
    public String flywayFingerprint() throws SQLException {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT version, description, success FROM " + Identifiers.quote(schema)
                + ".flyway_schema_history ORDER BY installed_rank";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                sb.append(rs.getString(1)).append('|').append(rs.getString(2))
                        .append('|').append(rs.getBoolean(3)).append('\n');
            }
        }
        return Digests.sha256Hex(sb.toString());
    }

    public boolean tableExists(String table) throws SQLException {
        String sql = "SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace"
                + " WHERE n.nspname=? AND c.relname=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long rowCount(String table) throws SQLException {
        Identifiers.requireSqlIdentifier(table, "table");
        String sql = "SELECT count(*) FROM " + Identifiers.quote(schema) + "." + Identifiers.quote(table);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    /** Sequences owned by this schema, mapped to the column they feed. */
    public Map<String, String[]> sequences() throws SQLException {
        Map<String, String[]> out = new TreeMap<>();
        String sql = """
                SELECT s.relname, t.relname, a.attname
                FROM pg_class s
                JOIN pg_namespace n ON n.oid = s.relnamespace
                JOIN pg_depend d ON d.objid = s.oid AND d.classid = 'pg_class'::regclass AND d.deptype IN ('a','i')
                JOIN pg_class t ON t.oid = d.refobjid
                JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = d.refobjsubid
                WHERE n.nspname = ? AND s.relkind = 'S'
                ORDER BY s.relname
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), new String[]{rs.getString(2), rs.getString(3)});
                }
            }
        }
        return out;
    }

    /** Admin-managed indexes already present. Reported as a warning; never removed by the importer. */
    public java.util.Set<String> nonConstraintIndexes() throws SQLException {
        java.util.Set<String> out = new TreeSet<>();
        String sql = """
                SELECT c.relname
                FROM pg_index i
                JOIN pg_class c ON c.oid = i.indexrelid
                JOIN pg_class t ON t.oid = i.indrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = ? AND NOT i.indisprimary AND NOT i.indisunique
                ORDER BY c.relname
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
        }
        return out;
    }
}
