package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps autovacuum off the tables being loaded until the import is done with them.
 *
 * <p>Since PostgreSQL 13 inserts alone trigger autovacuum, so a bulk load starts vacuums of tables
 * that are still being written, and of the rows the rebuild and {@code ANALYZE} are about to read,
 * competing with both for the disk. The setting is per leaf table (a partitioned parent takes no
 * storage parameters) and is reset when the import finishes; validation fails on any table left
 * with it.
 */
public final class Autovacuum {

    static final String OFF = "autovacuum_enabled=false";

    private Autovacuum() {
    }

    /** @return the number of tables changed */
    static int disable(Connection pg, String schema, List<String> tables) throws SQLException {
        return alter(pg, schema, tables, "SET (autovacuum_enabled = false, toast.autovacuum_enabled = false)");
    }

    /** @return the number of tables changed */
    static int restore(Connection pg, String schema, List<String> tables) throws SQLException {
        return alter(pg, schema, tables, "RESET (autovacuum_enabled, toast.autovacuum_enabled)");
    }

    /** Tables in the schema that still have autovacuum switched off. */
    public static List<String> disabledTables(Connection pg, String schema) throws SQLException {
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = pg.prepareStatement("SELECT c.relname FROM pg_class c"
                + " JOIN pg_namespace n ON n.oid = c.relnamespace"
                + " WHERE n.nspname = ? AND c.relkind = 'r' AND ? = ANY (c.reloptions) ORDER BY c.relname")) {
            ps.setString(1, schema);
            ps.setString(2, OFF);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
        }
        return out;
    }

    private static int alter(Connection pg, String schema, List<String> tables, String change) throws SQLException {
        List<String> leaves = leafTables(pg, schema, tables);
        try (Statement st = pg.createStatement()) {
            for (String table : leaves) {
                st.execute("ALTER TABLE " + Identifiers.quote(schema) + "." + Identifiers.quote(table) + " " + change);
            }
        }
        return leaves.size();
    }

    /** The tables themselves when ordinary, and every partition below them. */
    static List<String> leafTables(Connection pg, String schema, List<String> tables) throws SQLException {
        List<String> out = new ArrayList<>();
        String sql = """
                WITH RECURSIVE tree AS (
                    SELECT c.oid, c.relname, c.relkind FROM pg_class c
                    JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE n.nspname = ? AND c.relname = ANY (?)
                    UNION ALL
                    SELECT c.oid, c.relname, c.relkind FROM pg_inherits h
                    JOIN tree t ON t.oid = h.inhparent
                    JOIN pg_class c ON c.oid = h.inhrelid
                )
                SELECT DISTINCT relname FROM tree WHERE relkind = 'r' ORDER BY relname
                """;
        try (PreparedStatement ps = pg.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setArray(2, pg.createArrayOf("text", tables.toArray()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
        }
        return out;
    }
}
