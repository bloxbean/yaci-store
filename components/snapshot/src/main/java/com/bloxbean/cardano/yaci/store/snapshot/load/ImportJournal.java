package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Durable record of import progress, living in the target schema so it survives any kind of crash.
 *
 * <p>A batch's completion row is written in the same transaction as the batch's data, so a resumed
 * import can trust it exactly: a recorded batch is committed, an unrecorded batch has no rows.
 *
 * <p>The journal is dropped only after validation passes, never as an automatic recovery action.
 */
public class ImportJournal {

    public static final String BATCH_TABLE = "_yaci_snapshot_import";
    public static final String RUN_TABLE = "_yaci_snapshot_import_run";

    public static final String STATUS_LOADING = "LOADING";
    public static final String STATUS_VALIDATING = "VALIDATING";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_FAILED = "FAILED";

    private final Connection conn;
    private final String schema;

    public ImportJournal(Connection conn, String schema) {
        this.conn = conn;
        this.schema = Identifiers.requireSqlIdentifier(schema, "target schema");
    }

    private String q(String table) {
        return Identifiers.quote(schema) + "." + Identifiers.quote(table);
    }

    public void createIfAbsent() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS " + q(BATCH_TABLE) + " ("
                    + " snapshot_id varchar(64) NOT NULL,"
                    + " batch_id varchar(64) NOT NULL,"
                    + " spec_id varchar(100) NOT NULL,"
                    + " target_table varchar(100) NOT NULL,"
                    + " row_count bigint,"
                    + " completed_at timestamp NOT NULL DEFAULT now(),"
                    + " PRIMARY KEY (snapshot_id, batch_id))");
            st.execute("CREATE TABLE IF NOT EXISTS " + q(RUN_TABLE) + " ("
                    + " snapshot_id varchar(64) PRIMARY KEY,"
                    + " manifest_digest varchar(64) NOT NULL,"
                    + " network varchar(50),"
                    + " protocol_magic bigint,"
                    + " point_epoch integer,"
                    + " point_slot bigint,"
                    + " point_block bigint,"
                    + " point_hash varchar(64),"
                    + " schema_fingerprint varchar(64),"
                    + " scope_digest varchar(64),"
                    + " status varchar(20) NOT NULL,"
                    + " started_at timestamp NOT NULL DEFAULT now(),"
                    + " updated_at timestamp NOT NULL DEFAULT now(),"
                    + " message text)");
            st.execute(addRunColumnSql("scope_digest", "varchar(64)"));
            st.execute(addRunColumnSql("transforms", "text"));
            st.execute(addRunColumnSql("retention", "text"));
        }
    }

    /** Adds a column a newer release records to an older journal; {@code column} and {@code type} are constants. */
    private String addRunColumnSql(String column, String type) {
        return "ALTER TABLE " + q(RUN_TABLE) + " ADD COLUMN IF NOT EXISTS "
                + Identifiers.quote(Identifiers.requireSqlIdentifier(column, "journal column")) + " " + type;
    }

    public boolean exists() throws SQLException {
        String sql = "SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace"
                + " WHERE n.nspname=? AND c.relname=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, RUN_TABLE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public record Run(String snapshotId, String manifestDigest, String network, long protocolMagic,
                      int pointEpoch, long pointSlot, long pointBlock, String pointHash,
                      String schemaFingerprint, String status, String message, String scopeDigest,
                      String retention) {
        public Run(String snapshotId, String manifestDigest, String network, long protocolMagic,
                   int pointEpoch, long pointSlot, long pointBlock, String pointHash,
                   String schemaFingerprint, String status, String message, String scopeDigest) {
            this(snapshotId, manifestDigest, network, protocolMagic, pointEpoch, pointSlot, pointBlock,
                    pointHash, schemaFingerprint, status, message, scopeDigest, null);
        }

        public Run(String snapshotId, String manifestDigest, String network, long protocolMagic,
                   int pointEpoch, long pointSlot, long pointBlock, String pointHash,
                   String schemaFingerprint, String status, String message) {
            this(snapshotId, manifestDigest, network, protocolMagic, pointEpoch, pointSlot, pointBlock,
                    pointHash, schemaFingerprint, status, message, null);
        }
    }

    public Optional<Run> currentRun() throws SQLException {
        if (!exists()) {
            return Optional.empty();
        }
        boolean hasScope = hasColumn("scope_digest");
        boolean hasRetention = hasColumn("retention");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(currentRunSql(hasScope, hasRetention))) {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new Run(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4),
                    rs.getInt(5), rs.getLong(6), rs.getLong(7), rs.getString(8), rs.getString(9),
                    rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13)));
        }
    }

    /** The newest run; a journal from before a column was added reads it as null. */
    private String currentRunSql(boolean hasScope, boolean hasRetention) {
        return "SELECT snapshot_id, manifest_digest, network, protocol_magic,"
                + " point_epoch, point_slot, point_block, point_hash, schema_fingerprint, status, message,"
                + (hasScope ? "scope_digest" : "NULL AS scope_digest")
                + (hasRetention ? ", retention" : ", NULL AS retention")
                + " FROM " + q(RUN_TABLE) + " ORDER BY started_at DESC LIMIT 1";
    }

    private boolean hasColumn(String column) throws SQLException {
        try (var ps = conn.prepareStatement("SELECT 1 FROM information_schema.columns"
                + " WHERE table_schema=? AND table_name=? AND column_name=?")) {
            ps.setString(1, schema);
            ps.setString(2, RUN_TABLE);
            ps.setString(3, column);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Every value is a bind parameter; only the validated, quoted schema is part of the text. */
    private String startRunSql() {
        return "INSERT INTO " + q(RUN_TABLE) + " (snapshot_id, manifest_digest, network,"
                + " protocol_magic, point_epoch, point_slot, point_block, point_hash,"
                + " schema_fingerprint, status, scope_digest, retention)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"
                + " ON CONFLICT (snapshot_id) DO UPDATE SET status = EXCLUDED.status, updated_at = now()";
    }

    public void startRun(Run run) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(startRunSql())) {
            ps.setString(1, run.snapshotId());
            ps.setString(2, run.manifestDigest());
            ps.setString(3, run.network());
            ps.setLong(4, run.protocolMagic());
            ps.setInt(5, run.pointEpoch());
            ps.setLong(6, run.pointSlot());
            ps.setLong(7, run.pointBlock());
            ps.setString(8, run.pointHash());
            ps.setString(9, run.schemaFingerprint());
            ps.setString(10, run.status());
            ps.setString(11, run.scopeDigest());
            ps.setString(12, run.retention());
            ps.executeUpdate();
        }
    }

    /**
     * Record which import transform loads each table: {@code id=v<transform-version>:<spec digest>}.
     * The manifest pins only the export contract, so this is where the transform that actually
     * produced the rows is kept.
     */
    public void recordTransforms(String snapshotId, Collection<SnapshotTableSpec> specs) throws SQLException {
        String transforms = specs.stream()
                .map(s -> s.id() + "=v" + s.importSpec().transformVersion() + ":" + s.digest())
                .sorted().collect(Collectors.joining(","));
        try (PreparedStatement ps = conn.prepareStatement("UPDATE " + q(RUN_TABLE)
                + " SET transforms=? WHERE snapshot_id=?")) {
            ps.setString(1, transforms);
            ps.setString(2, snapshotId);
            ps.executeUpdate();
        }
    }

    /** What {@link #recordTransforms} recorded for a run, if anything. */
    public Optional<String> transforms(String snapshotId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT transforms FROM " + q(RUN_TABLE)
                + " WHERE snapshot_id=?")) {
            ps.setString(1, snapshotId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
            }
        }
    }

    public void setStatus(String snapshotId, String status, String message) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE " + q(RUN_TABLE)
                + " SET status=?, message=?, updated_at=now() WHERE snapshot_id=?")) {
            ps.setString(1, status);
            ps.setString(2, message);
            ps.setString(3, snapshotId);
            ps.executeUpdate();
        }
    }

    public Set<String> completedBatchIds(String snapshotId) throws SQLException {
        Set<String> out = new TreeSet<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT batch_id FROM " + q(BATCH_TABLE) + " WHERE snapshot_id=?")) {
            ps.setString(1, snapshotId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
        }
        return out;
    }

    public Map<String, Long> rowsPerTable(String snapshotId) throws SQLException {
        Map<String, Long> out = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT target_table, sum(row_count)"
                + " FROM " + q(BATCH_TABLE) + " WHERE snapshot_id=? GROUP BY target_table ORDER BY 1")) {
            ps.setString(1, snapshotId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), rs.getLong(2));
                }
            }
        }
        return out;
    }

    public long completedCount(String snapshotId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT count(*) FROM " + q(BATCH_TABLE) + " WHERE snapshot_id=?")) {
            ps.setString(1, snapshotId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    /** Removed only once validation has passed; never as an automatic recovery step. */
    public void drop() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS " + q(BATCH_TABLE));
            st.execute("DROP TABLE IF EXISTS " + q(DeferredIndexes.TABLE));
            st.execute("DROP TABLE IF EXISTS " + q(RUN_TABLE));
        }
    }

    /** Fully qualified journal table name for use inside the DuckDB-driven batch transaction. */
    public static String qualifiedBatchTable(String duckDbAlias, String schema) {
        return duckDbAlias + "." + Identifiers.quote(schema) + "." + Identifiers.quote(BATCH_TABLE);
    }
}
