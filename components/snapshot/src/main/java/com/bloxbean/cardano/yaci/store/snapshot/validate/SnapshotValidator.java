package com.bloxbean.cardano.yaci.store.snapshot.validate;

import com.bloxbean.cardano.yaci.store.snapshot.load.ImportJournal;
import com.bloxbean.cardano.yaci.store.snapshot.load.PgSchema;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ConsistencyPoint;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.CutoffType;
import com.bloxbean.cardano.yaci.store.snapshot.spec.RestoreMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Offline validation of a restored database.
 *
 * <p>A checksum proves transport integrity; it says nothing about whether the database can resume
 * chain sync. These checks are the difference: the manifest point really is the last imported block
 * and the final cursor, the retained block tail is a continuous chain, nothing sits beyond a table's
 * declared cutoff, and the reconstructed control state is coherent.
 */
public class SnapshotValidator {

    private final SnapshotSpecRegistry registry;

    public SnapshotValidator(SnapshotSpecRegistry registry) {
        this.registry = registry;
    }

    /** Level 2: schema and load validation. */
    public ValidationReport validateLoad(Connection pg, String schema, SnapshotManifest manifest)
            throws SQLException {
        ValidationReport.Builder b = new ValidationReport.Builder("schema-and-load");
        PgSchema pgs = new PgSchema(pg, schema);

        String fingerprint = pgs.fingerprint();
        b.check("schema-fingerprint",
                manifest.schemaFingerprint() == null || manifest.schemaFingerprint().equals(fingerprint),
                manifest.schemaFingerprint() == null ? "not recorded in manifest" : fingerprint);
        b.check("flyway-fingerprint",
                manifest.flywayFingerprint() == null
                        || manifest.flywayFingerprint().equals(pgs.flywayFingerprint()),
                manifest.flywayFingerprint() == null ? "not recorded in manifest" : "matches");

        for (SnapshotManifest.TableManifest t : manifest.tables()) {
            SnapshotTableSpec spec = registry.byId(t.specId()).orElse(null);
            if (spec == null || spec.restore() != RestoreMode.IMPORT) {
                continue;
            }
            if (!pgs.tableExists(t.targetTable())) {
                // A module the target schema does not enable. Only a problem if the snapshot
                // actually carries rows for it.
                b.check("rows:" + t.targetTable(), t.rowCount() == 0,
                        "table not present in the target schema, manifest declares " + t.rowCount()
                                + " row(s)");
                continue;
            }
            long actual = count(pg, schema, t.targetTable());
            b.check("rows:" + t.targetTable(), actual == t.rowCount(),
                    actual + " rows (manifest declares " + t.rowCount() + ")");
        }

        // Tables the specification says must stay empty really are empty.
        for (SnapshotTableSpec spec : registry.all()) {
            if (spec.restore() != RestoreMode.EMPTY_EXPECTED && spec.restore() != RestoreMode.NOT_RESTORED) {
                continue;
            }
            if (!pgs.tableExists(spec.targetTable())) {
                continue;
            }
            long n = count(pg, schema, spec.targetTable());
            b.check("empty:" + spec.targetTable(), n == 0, n + " rows");
        }

        Map<String, String[]> sequences = pgs.sequences();
        for (Map.Entry<String, String[]> e : sequences.entrySet()) {
            String seq = e.getKey();
            String table = e.getValue()[0];
            String column = e.getValue()[1];
            long last = scalar(pg, "SELECT last_value FROM " + Identifiers.quote(schema) + "."
                    + Identifiers.quote(seq));
            long max = scalar(pg, "SELECT COALESCE(max(" + Identifiers.quote(column) + "), 0) FROM "
                    + Identifiers.quote(schema) + "." + Identifiers.quote(table));
            b.check("sequence:" + seq, last >= max,
                    "last_value " + last + " vs max(" + table + "." + column + ") " + max);
        }

        ImportJournal journal = new ImportJournal(pg, schema);
        if (journal.exists()) {
            long completed = journal.completedCount(manifest.snapshotId());
            b.pass("import-journal", completed + " completed batch(es) recorded");
        } else {
            b.fail("import-journal", "no import journal found for this schema");
        }

        return b.build();
    }

    /** Level 3: semantic validation. */
    public ValidationReport validateSemantics(Connection pg, String schema, SnapshotManifest manifest,
                                              long eventPublisherId, int cursorBlocksToKeep)
            throws SQLException {
        ValidationReport.Builder b = new ValidationReport.Builder("semantic");
        ConsistencyPoint point = manifest.point();

        long maxBlock = scalar(pg, "SELECT COALESCE(max(number), -1) FROM " + qt(schema, "block"));
        b.check("point-is-last-block", maxBlock == point.blockNumber(),
                "max(block.number)=" + maxBlock + ", manifest point=" + point.blockNumber());

        String hashAtPoint = string(pg, "SELECT hash FROM " + qt(schema, "block") + " WHERE number = "
                + point.blockNumber());
        b.check("point-hash-matches", point.blockHash().equals(hashAtPoint),
                "block " + point.blockNumber() + " hash " + hashAtPoint);

        try (PreparedStatement ps = pg.prepareStatement("SELECT block_hash, slot, block_number"
                + " FROM " + qt(schema, "cursor_") + " WHERE id = ? ORDER BY slot DESC LIMIT 1")) {
            ps.setLong(1, eventPublisherId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean ok = point.blockHash().equals(rs.getString(1))
                            && point.slot() == rs.getLong(2)
                            && point.blockNumber() == rs.getLong(3);
                    b.check("final-cursor-matches-point", ok,
                            "cursor block " + rs.getLong(3) + " slot " + rs.getLong(2));
                } else {
                    b.fail("final-cursor-matches-point", "no cursor row for publisher " + eventPublisherId);
                }
            }
        }

        long cursorRows = scalarPrepared(pg, "SELECT count(*) FROM " + qt(schema, "cursor_")
                + " WHERE id = ?", eventPublisherId);
        b.check("cursor-tail-depth", cursorRows >= Math.min(cursorBlocksToKeep, maxBlock + 1),
                cursorRows + " cursor row(s), configured retention " + cursorBlocksToKeep);

        // A gap in the retained tail would break the normal restart rollback walk.
        long chainBreaks = scalarPrepared(pg,
                "SELECT count(*) FROM (SELECT c.block_number, c.prev_block_hash,"
                        + " lag(c.block_hash) OVER (ORDER BY c.block_number) AS previous_hash"
                        + " FROM " + qt(schema, "cursor_") + " c WHERE c.id = ?) t"
                        + " WHERE previous_hash IS NOT NULL AND previous_hash <> prev_block_hash",
                eventPublisherId);
        b.check("cursor-tail-chain", chainBreaks == 0, chainBreaks + " prev-hash discontinuity(ies)");

        long blockChainBreaks = scalar(pg,
                "SELECT count(*) FROM (SELECT b.number, b.prev_hash,"
                        + " lag(b.hash) OVER (ORDER BY b.number) AS previous_hash"
                        + " FROM " + qt(schema, "block") + " b"
                        + " WHERE b.number > " + (point.blockNumber() - cursorBlocksToKeep)
                        + "   AND b.number <= " + point.blockNumber() + ") t"
                        + " WHERE previous_hash IS NOT NULL AND previous_hash <> prev_hash");
        b.check("block-tail-chain", blockChainBreaks == 0,
                blockChainBreaks + " prev-hash discontinuity(ies) in the retained block tail");

        PgSchema pgs = new PgSchema(pg, schema);
        for (SnapshotTableSpec spec : registry.importedTables()) {
            SnapshotTableSpec.CutoffRule cutoff = spec.consistency().cutoff();
            if (cutoff.type() == CutoffType.NONE || !pgs.tableExists(spec.targetTable())) {
                continue;
            }
            long limit = switch (cutoff.type()) {
                case SLOT_LTE -> point.slot();
                case EPOCH_LTE -> point.epoch();
                case EPOCH_LTE_OFFSET -> point.epoch() - cutoff.offset();
                case NONE -> Long.MAX_VALUE;
            };
            long beyond = scalar(pg, "SELECT count(*) FROM " + qt(schema, spec.targetTable())
                    + " WHERE " + Identifiers.quote(cutoffTargetColumn(spec)) + " > " + limit);
            b.check("cutoff:" + spec.targetTable(), beyond == 0,
                    beyond + " row(s) beyond " + cutoffTargetColumn(spec) + " <= " + limit);
        }

        long eraRows = scalar(pg, "SELECT count(*) FROM " + qt(schema, "era"));
        b.check("era-reconstructed", eraRows > 0, eraRows + " era transition(s)");
        long eraMismatch = scalar(pg, "SELECT count(*) FROM " + qt(schema, "era") + " e"
                + " LEFT JOIN " + qt(schema, "block") + " b ON b.number = e.block"
                + " WHERE b.hash IS NULL OR b.hash <> e.block_hash OR b.slot <> e.start_slot"
                + "    OR b.era <> e.era");
        b.check("era-points-at-real-blocks", eraMismatch == 0, eraMismatch + " inconsistent era row(s)");

        // A Byron era row breaks EraService.getEraForEpoch(), which walks every era row and cannot
        // derive an epoch from a Byron slot. A normally synced database has no such row.
        long byronEraRows = scalar(pg, "SELECT count(*) FROM " + qt(schema, "era") + " WHERE era <= 1");
        b.check("era-excludes-byron", byronEraRows == 0,
                byronEraRows + " Byron era row(s); a synced database has none and the AdaPot job "
                        + "fails if one is present");

        // The account balance watermark gates AccountBalanceProcessor, which in turn feeds the
        // AdaPot stake snapshot. At block 0 it reports a multi-million block gap and skips.
        if (pgs.tableExists("account_config")) {
            long watermark = scalar(pg, "SELECT COALESCE(max(block), -1) FROM " + qt(schema, "account_config")
                    + " WHERE config_id = 'last_account_balance_processed_block'");
            b.check("account-balance-watermark", watermark == point.blockNumber(),
                    "watermark block " + watermark + ", snapshot point " + point.blockNumber());
        }

        validateAddressUtxo(pg, schema, b);

        long epochMax = scalar(pg, "SELECT COALESCE(max(number), -1) FROM " + qt(schema, "epoch"));
        b.check("epoch-aggregate-at-point", epochMax <= point.epoch(),
                "max(epoch.number)=" + epochMax + ", point epoch=" + point.epoch());

        return b.build();
    }

    private void validateAddressUtxo(Connection pg, String schema, ValidationReport.Builder b)
            throws SQLException {
        String t = qt(schema, "address_utxo");
        long total = scalar(pg, "SELECT count(*) FROM " + t);
        if (total == 0) {
            b.pass("address-utxo", "table is empty");
            return;
        }
        long duplicates = scalar(pg, "SELECT count(*) FROM (SELECT tx_hash, output_index FROM " + t
                + " GROUP BY tx_hash, output_index HAVING count(*) > 1) d");
        b.check("address-utxo-unique", duplicates == 0,
                duplicates + " duplicate (tx_hash, output_index) key(s)");

        long badJson = scalar(pg, "SELECT count(*) FROM " + t
                + " WHERE amounts IS NULL OR jsonb_typeof(amounts) <> 'array'"
                + "    OR jsonb_array_length(amounts) = 0");
        b.check("address-utxo-amounts-shape", badJson == 0, badJson + " row(s) with invalid amounts");

        long lovelaceMismatch = scalar(pg, "SELECT count(*) FROM " + t + " u"
                + " WHERE u.lovelace_amount IS DISTINCT FROM ("
                + "   SELECT COALESCE(sum((e->>'quantity')::numeric), 0)::bigint FROM jsonb_array_elements(u.amounts) e"
                + "   WHERE e->>'unit' = 'lovelace')");
        b.check("address-utxo-lovelace-total", lovelaceMismatch == 0,
                lovelaceMismatch + " row(s) whose lovelace_amount disagrees with amounts");

        long multiLovelace = scalar(pg, "SELECT count(*) FROM " + t + " u WHERE ("
                + " SELECT count(*) FROM jsonb_array_elements(u.amounts) e WHERE e->>'unit' = 'lovelace') > 1");
        b.check("address-utxo-single-lovelace", multiLovelace == 0,
                multiLovelace + " row(s) with more than one lovelace entry");

        long missingBlock = scalar(pg, "SELECT count(*) FROM " + t + " WHERE block IS NULL");
        b.check("address-utxo-block-resolved", missingBlock == 0,
                missingBlock + " row(s) whose block number could not be recovered");
    }

    /** The cutoff column names the source; find the target column it became. */
    private String cutoffTargetColumn(SnapshotTableSpec spec) {
        String sourceColumn = spec.consistency().cutoff().column();
        for (Map.Entry<String, SnapshotTableSpec.Column> e : spec.importSpec().columns().entrySet()) {
            if (sourceColumn.equals(e.getValue().source())) {
                return e.getKey();
            }
        }
        return sourceColumn;
    }

    private static String qt(String schema, String table) {
        return Identifiers.quote(schema) + "." + Identifiers.quote(table);
    }

    private static long count(Connection pg, String schema, String table) throws SQLException {
        return scalar(pg, "SELECT count(*) FROM " + qt(schema, table));
    }

    private static long scalar(Connection pg, String sql) throws SQLException {
        try (Statement st = pg.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private static long scalarPrepared(Connection pg, String sql, long param) throws SQLException {
        try (PreparedStatement ps = pg.prepareStatement(sql)) {
            ps.setLong(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    private static String string(Connection pg, String sql) throws SQLException {
        try (Statement st = pg.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    /** Level 4 is an operational gate, not an offline check; it is listed so the report is complete. */
    public static List<String> applicationAcceptanceChecklist() {
        return List.of(
                "start the matching release with store.sync-auto-start=false and run read/API smoke tests",
                "start against a Cardano node and confirm the snapshot point is accepted",
                "observe the normal startup rollback and replay behaviour",
                "sync past the snapshot point, restart gracefully, and confirm the same tip",
                "kill the process during sync, restart, and confirm recovery",
                "exercise a rollback within the retained cursor history");
    }

    /** Convenience for the CLI: run every offline level and merge the results. */
    public List<ValidationReport> validateAll(Connection pg, String schema, SnapshotManifest manifest,
                                              long eventPublisherId, int cursorBlocksToKeep)
            throws SQLException {
        List<ValidationReport> reports = new ArrayList<>();
        reports.add(validateLoad(pg, schema, manifest));
        reports.add(validateSemantics(pg, schema, manifest, eventPublisherId, cursorBlocksToKeep));
        return reports;
    }
}
