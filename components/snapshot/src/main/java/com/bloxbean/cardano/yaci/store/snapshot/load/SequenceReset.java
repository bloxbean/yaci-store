package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resets every sequence that feeds an imported column.
 *
 * <p>Bulk loading writes explicit key values, which leaves the owning sequence at its initial value.
 * The first row the restored instance inserts would then collide. {@code setval} is applied with the
 * empty-table semantics PostgreSQL expects: {@code setval(seq, max, true)} when rows exist, and
 * {@code setval(seq, 1, false)} when the table is empty, so the next value is 1.
 */
public class SequenceReset {

    private static final Logger log = LoggerFactory.getLogger(SequenceReset.class);

    private final Connection conn;
    private final String schema;

    public SequenceReset(Connection conn, String schema) {
        this.conn = conn;
        this.schema = Identifiers.requireSqlIdentifier(schema, "target schema");
    }

    /**
     * @return sequence name to the value it was set to
     */
    public Map<String, Long> resetAll() throws SQLException {
        Map<String, Long> result = new LinkedHashMap<>();
        Map<String, String[]> sequences = new PgSchema(conn, schema).sequences();

        for (Map.Entry<String, String[]> e : sequences.entrySet()) {
            String sequence = e.getKey();
            String table = e.getValue()[0];
            String column = e.getValue()[1];
            Identifiers.requireSqlIdentifier(sequence, "sequence");
            Identifiers.requireSqlIdentifier(table, "table");
            Identifiers.requireSqlIdentifier(column, "column");

            String qualifiedSeq = Identifiers.literal(schema + "." + sequence);
            String sql = "SELECT setval(" + qualifiedSeq + ","
                    + " COALESCE((SELECT max(" + Identifiers.quote(column) + ") FROM "
                    + Identifiers.quote(schema) + "." + Identifiers.quote(table) + "), 1),"
                    + " (SELECT max(" + Identifiers.quote(column) + ") IS NOT NULL FROM "
                    + Identifiers.quote(schema) + "." + Identifiers.quote(table) + "))";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) {
                    long value = rs.getLong(1);
                    result.put(sequence, value);
                    log.debug("Reset sequence {} to {} (from {}.{})", sequence, value, table, column);
                }
            }
        }
        return result;
    }
}
