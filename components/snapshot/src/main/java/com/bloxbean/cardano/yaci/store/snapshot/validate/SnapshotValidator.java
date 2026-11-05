package com.bloxbean.cardano.yaci.store.snapshot.validate;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.ManifestCodec;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ManifestValidator;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.Retention;
import com.bloxbean.cardano.yaci.store.snapshot.load.Autovacuum;
import com.bloxbean.cardano.yaci.store.snapshot.load.DeferredIndexes;
import com.bloxbean.cardano.yaci.store.snapshot.load.ImportJournal;
import com.bloxbean.cardano.yaci.store.snapshot.load.PgSchema;
import com.bloxbean.cardano.yaci.store.snapshot.load.PostgresTuning;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ConsistencyPoint;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.CutoffType;
import com.bloxbean.cardano.yaci.store.snapshot.spec.RestoreMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;
import com.bloxbean.cardano.yaci.store.snapshot.util.Progress;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Offline validation of a restored database.
 *
 * <p>A checksum proves transport integrity; it says nothing about whether the database can resume
 * chain sync. These checks are the difference: the manifest point really is the last imported block
 * and the final cursor, the retained block tail is a continuous chain, nothing sits beyond a table's
 * declared cutoff, and the reconstructed control state is coherent.
 */
public class SnapshotValidator {

    /** Phase banners {@link #validateAll} sends, so a caller can number them. */
    public static final int PHASES = 2;

    private final SnapshotSpecRegistry registry;

    public SnapshotValidator(SnapshotSpecRegistry registry) {
        this.registry = registry;
    }

    /** Level 2: schema and load validation. */
    public ValidationReport validateLoad(Connection pg, String schema, SnapshotManifest manifest)
            throws SQLException {
        SnapshotManifest view = importView(pg, schema, manifest);
        return validateLoad(pg, schema, view, new Scans(pg, schema, view, null, 1, m -> { }));
    }

    private ValidationReport validateLoad(Connection pg, String schema, SnapshotManifest manifest, Scans scans)
            throws SQLException {
        ValidationReport.Builder b = new ValidationReport.Builder("schema-and-load");
        for (String problem : new ManifestValidator(registry)
                .validate(manifest)) {
            b.fail("manifest", problem);
        }
        if (!b.build().passed()) {
            return b.build();
        }
        PgSchema pgs = new PgSchema(pg, schema);

        var compatibility = com.bloxbean.cardano.yaci.store.snapshot.load.SchemaCompatibility.check(pgs, manifest, registry);
        b.check("schema-fingerprint", compatibility.isEmpty(), String.join("; ", compatibility));

        for (SnapshotManifest.TableManifest t : manifest.tables()) {
            SnapshotTableSpec spec = registry.byId(t.specId()).orElse(null);
            if (spec == null || !registry.isSelected(spec.id()) || spec.restore() != RestoreMode.IMPORT) {
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
            TableScanner.Result scan = scans.get().get(t.targetTable());
            b.check("rows:" + t.targetTable(), scan.rows() == t.rowCount(),
                    scan.rows() + " rows (manifest declares " + t.rowCount() + ")");
            for (int i = 0; i < spec.validation().requiredColumns().size(); i++) {
                String column = spec.validation().requiredColumns().get(i);
                long nulls = scan.nulls().get(i);
                b.check("required:" + spec.targetTable() + "." + column, nulls == 0,
                        nulls + " null value(s)");
            }
        }

        for (var spec : registry.knownTables()) {
            if (!registry.isSelected(spec.id()) && spec.restore() == RestoreMode.IMPORT && pgs.tableExists(spec.targetTable())) {
                long rows = count(pg, schema, spec.targetTable());
                b.check("disabled-empty:" + spec.targetTable(), rows == 0, rows + " rows in a disabled store");
            }
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
            if (registry.isScoped() && registry.byTarget(table).filter(s -> registry.isSelected(s.id())).isEmpty()) continue;
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
            ImportJournal.Run run = journal.currentRun().orElse(null);
            b.check("import-scope", run != null && (!(registry.isScoped() || run.scopeDigest() != null)
                    || registry.scopeDigest().equals(run.scopeDigest())), "enabled stores must match the original import");
            b.check("import-journal", run != null && run.manifestDigest().equals(
                    new ManifestCodec().digest(manifest)),
                    "journal must match the exact manifest being validated");
        } else {
            b.fail("import-journal", "no import journal found for this schema");
        }

        List<String> noAutovacuum = Autovacuum.disabledTables(pg, schema);
        b.check("autovacuum-restored", noAutovacuum.isEmpty(), noAutovacuum.isEmpty()
                ? "no table is left with autovacuum switched off by the import"
                : noAutovacuum.size() + " table(s) with autovacuum off: "
                        + String.join(", ", noAutovacuum.subList(0, Math.min(5, noAutovacuum.size()))));

        b.check("postgres-settings-restored", !PostgresTuning.pending(pg, schema),
                "server settings changed by --tune-postgres are back to their original values");

        List<String> indexProblems = new DeferredIndexes(pg, schema).problems(manifest.snapshotId());
        b.check("deferred-indexes", indexProblems.isEmpty(), indexProblems.isEmpty()
                ? "every key and index dropped for the load is back, as captured"
                : indexProblems.size() + " problem(s): " + String.join("; ", indexProblems.subList(0, Math.min(5, indexProblems.size()))));

        return b.build();
    }

    /** Level 3: semantic validation. */
    public ValidationReport validateSemantics(Connection pg, String schema, SnapshotManifest manifest,
                                              long eventPublisherId, int cursorBlocksToKeep)
            throws SQLException {
        return validateSemantics(pg, schema, manifest, eventPublisherId, cursorBlocksToKeep,
                new Scans(pg, schema, manifest, null, 1, m -> { }));
    }

    private ValidationReport validateSemantics(Connection pg, String schema, SnapshotManifest manifest,
                                               long eventPublisherId, int cursorBlocksToKeep, Scans scans)
            throws SQLException {
        ValidationReport.Builder b = new ValidationReport.Builder("semantic");
        ConsistencyPoint point = manifest.point();

        // Without the blocks store there is no block table to check the point against: the cursor and
        // era were rebuilt from the snapshot's block files, and the cursor is checked against the point.
        boolean blocks = registry.byId("block").map(s -> registry.isSelected(s.id())).orElse(true);
        long maxBlock = point.blockNumber();
        if (blocks) {
            maxBlock = scalar(pg, "SELECT COALESCE(max(number), -1) FROM " + qt(schema, "block"));
            b.check("point-is-last-block", maxBlock == point.blockNumber(),
                    "max(block.number)=" + maxBlock + ", manifest point=" + point.blockNumber());

            String hashAtPoint = string(pg, "SELECT hash FROM " + qt(schema, "block") + " WHERE number = "
                    + point.blockNumber());
            b.check("point-hash-matches", point.blockHash().equals(hashAtPoint),
                    "block " + point.blockNumber() + " hash " + hashAtPoint);
        } else {
            b.pass("block-not-imported", "the blocks store is disabled; cursor and era were rebuilt from the"
                    + " snapshot's block files and are checked against the manifest point");
        }

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

        if (blocks) {
            long blockChainBreaks = scalar(pg,
                    "SELECT count(*) FROM (SELECT b.number, b.prev_hash,"
                            + " lag(b.hash) OVER (ORDER BY b.number) AS previous_hash"
                            + " FROM " + qt(schema, "block") + " b"
                            + " WHERE b.number > " + (point.blockNumber() - cursorBlocksToKeep)
                            + "   AND b.number <= " + point.blockNumber() + ") t"
                            + " WHERE previous_hash IS NOT NULL AND previous_hash <> prev_hash");
            b.check("block-tail-chain", blockChainBreaks == 0,
                    blockChainBreaks + " prev-hash discontinuity(ies) in the retained block tail");
        }

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
            TableScanner.Result scan = scans.get().get(spec.targetTable());
            long beyond = scan != null && scan.beyondCutoff() != null ? scan.beyondCutoff()
                    : scalar(pg, "SELECT count(*) FROM " + qt(schema, spec.targetTable())
                    + " WHERE " + Identifiers.quote(cutoffTargetColumn(spec)) + " > " + limit);
            b.check("cutoff:" + spec.targetTable(), beyond == 0,
                    beyond + " row(s) beyond " + cutoffTargetColumn(spec) + " <= " + limit);
        }

        long eraRows = scalar(pg, "SELECT count(*) FROM " + qt(schema, "era"));
        b.check("era-reconstructed", eraRows > 0, eraRows + " era transition(s)");
        if (blocks) {
            long eraMismatch = scalar(pg, "SELECT count(*) FROM " + qt(schema, "era") + " e"
                    + " LEFT JOIN " + qt(schema, "block") + " b ON b.number = e.block"
                    + " WHERE b.hash IS NULL OR b.hash <> e.block_hash OR b.slot <> e.start_slot"
                    + "    OR b.era <> e.era");
            b.check("era-points-at-real-blocks", eraMismatch == 0, eraMismatch + " inconsistent era row(s)");
        } else {
            long eraAfterPoint = scalar(pg, "SELECT count(*) FROM " + qt(schema, "era")
                    + " WHERE block > " + point.blockNumber() + " OR start_slot > " + point.slot());
            b.check("era-before-point", eraAfterPoint == 0, eraAfterPoint + " era row(s) after the snapshot point");
        }

        // A Byron era row breaks EraService.getEraForEpoch(), which walks every era row and cannot
        // derive an epoch from a Byron slot. A normally synced database has no such row.
        long byronEraRows = scalar(pg, "SELECT count(*) FROM " + qt(schema, "era") + " WHERE era <= 1");
        b.check("era-excludes-byron", byronEraRows == 0,
                byronEraRows + " Byron era row(s); a synced database has none and the AdaPot job "
                        + "fails if one is present");

        // The account balance watermark gates AccountBalanceProcessor, which in turn feeds the
        // AdaPot stake snapshot. At block 0 it reports a multi-million block gap and skips.
        if (registry.byTarget("account_config").filter(s -> registry.isSelected(s.id())).isPresent() && pgs.tableExists("account_config")) {
            long watermark = scalar(pg, "SELECT COALESCE(max(block), -1) FROM " + qt(schema, "account_config")
                    + " WHERE config_id = 'last_account_balance_processed_block'");
            b.check("account-balance-watermark", watermark == point.blockNumber(),
                    "watermark block " + watermark + ", snapshot point " + point.blockNumber());
        }

        if (registry.byTarget("address_utxo").filter(s -> registry.isSelected(s.id())).isPresent())
            validateAddressUtxo(pg, schema, b, scans);

        // The AdaPot job reads the cost model of the previous epoch's parameters at the first epoch
        // boundary after a restore, and fails outright when it is missing.
        if (pgs.tableExists("epoch_param") && pgs.tableExists("cost_model")) {
            long dangling = scalar(pg, "SELECT count(*) FROM " + qt(schema, "epoch_param") + " p"
                    + " WHERE p.cost_model_hash IS NOT NULL AND NOT EXISTS (SELECT 1 FROM "
                    + qt(schema, "cost_model") + " c WHERE c.hash = p.cost_model_hash)");
            b.check("epoch-param-cost-models", dangling == 0,
                    dangling + " epoch_param row(s) whose cost model is missing from cost_model");
        }

        if (registry.byTarget("epoch").filter(s -> registry.isSelected(s.id())).isPresent()) {
            long epochMax = scalar(pg, "SELECT COALESCE(max(number), -1) FROM " + qt(schema, "epoch"));
            b.check("epoch-aggregate-at-point", epochMax <= point.epoch(),
                    "max(epoch.number)=" + epochMax + ", point epoch=" + point.epoch());
        }

        return b.build();
    }

    private void validateAddressUtxo(Connection pg, String schema, ValidationReport.Builder b, Scans scans)
            throws SQLException {
        TableScanner.Result scan = scans.get().get("address_utxo");
        if (scan == null || scan.utxo() == null) {
            // Not part of this snapshot's loaded tables: read it on its own, still in one pass.
            scan = TableScanner.scanOne(pg, schema, new TableScanner.Target("address_utxo", List.of(), null, 0,
                    true, 0));
        }
        if (scan.rows() == 0) {
            b.pass("address-utxo", "table is empty");
            return;
        }
        // Rebuilding the key already proved uniqueness; only without it is a GROUP BY needed.
        String key = uniqueKeyOn(pg, schema, "address_utxo", List.of("output_index", "tx_hash"));
        if (key != null) {
            b.pass("address-utxo-unique", "enforced by unique index " + key);
        } else {
            long duplicates = scalar(pg, "SELECT count(*) FROM (SELECT tx_hash, output_index FROM "
                    + qt(schema, "address_utxo") + " GROUP BY tx_hash, output_index HAVING count(*) > 1) d");
            b.check("address-utxo-unique", duplicates == 0,
                    duplicates + " duplicate (tx_hash, output_index) key(s)");
        }

        TableScanner.UtxoResult u = scan.utxo();
        b.check("address-utxo-amounts-shape", u.badAmounts() == 0, u.badAmounts() + " row(s) with invalid amounts");
        b.check("address-utxo-lovelace-total", u.lovelaceMismatch() == 0,
                u.lovelaceMismatch() + " row(s) whose lovelace_amount disagrees with amounts");
        b.check("address-utxo-single-lovelace", u.multiLovelace() == 0,
                u.multiLovelace() + " row(s) with more than one lovelace entry");
        b.check("address-utxo-block-resolved", u.missingBlock() == 0,
                u.missingBlock() + " row(s) whose block number could not be recovered");
    }

    /** A valid, non-partial unique index on exactly these columns, or null. */
    private static String uniqueKeyOn(Connection pg, String schema, String table, List<String> sortedColumns)
            throws SQLException {
        try (PreparedStatement ps = pg.prepareStatement("SELECT i.relname FROM pg_index x"
                + " JOIN pg_class i ON i.oid = x.indexrelid JOIN pg_class c ON c.oid = x.indrelid"
                + " JOIN pg_namespace n ON n.oid = c.relnamespace"
                + " WHERE n.nspname = ? AND c.relname = ? AND x.indisunique AND x.indisvalid"
                + "   AND x.indpred IS NULL AND x.indexprs IS NULL"
                + "   AND (SELECT array_agg(a.attname::text ORDER BY a.attname) FROM pg_attribute a"
                + "        WHERE a.attrelid = c.oid AND a.attnum = ANY (x.indkey)) = ?::text[]"
                + " LIMIT 1")) {
            ps.setString(1, schema);
            ps.setString(2, table);
            ps.setArray(3, pg.createArrayOf("text", sortedColumns.toArray()));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /**
     * Per-table scan results, computed once on first use and shared by both levels: one read of every
     * loaded table instead of one per check.
     */
    private final class Scans {
        private final Connection pg;
        private final String schema;
        private final SnapshotManifest manifest;
        private final DeferredIndexes.ConnectionFactory connections;
        private final int workers;
        private final Consumer<String> progress;
        private Map<String, TableScanner.Result> results;

        Scans(Connection pg, String schema, SnapshotManifest manifest, DeferredIndexes.ConnectionFactory connections,
              int workers, Consumer<String> progress) {
            this.pg = pg;
            this.schema = schema;
            this.manifest = manifest;
            this.connections = connections;
            this.workers = workers;
            this.progress = progress;
        }

        Map<String, TableScanner.Result> get() throws SQLException {
            if (results == null) {
                results = TableScanner.scan(schema, targets(), pg, connections, workers, progress);
            }
            return results;
        }

        private List<TableScanner.Target> targets() throws SQLException {
            PgSchema pgs = new PgSchema(pg, schema);
            ConsistencyPoint point = manifest.point();
            boolean utxoSelected = registry.byTarget("address_utxo").filter(s -> registry.isSelected(s.id())).isPresent();
            List<TableScanner.Target> targets = new ArrayList<>();
            for (SnapshotManifest.TableManifest t : manifest.tables()) {
                SnapshotTableSpec spec = registry.byId(t.specId()).orElse(null);
                if (spec == null || !registry.isSelected(spec.id()) || spec.restore() != RestoreMode.IMPORT
                        || !pgs.tableExists(t.targetTable())) {
                    continue;
                }
                SnapshotTableSpec.CutoffRule cutoff = spec.consistency().cutoff();
                String cutoffColumn = cutoff.type() == CutoffType.NONE ? null : cutoffTargetColumn(spec);
                long limit = switch (cutoff.type()) {
                    case SLOT_LTE -> point.slot();
                    case EPOCH_LTE -> point.epoch();
                    case EPOCH_LTE_OFFSET -> point.epoch() - cutoff.offset();
                    case NONE -> Long.MAX_VALUE;
                };
                targets.add(new TableScanner.Target(t.targetTable(), spec.validation().requiredColumns(),
                        cutoffColumn, limit, utxoSelected && "address_utxo".equals(t.targetTable()), t.rowCount()));
            }
            return targets;
        }
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

    /** Runs every offline level on one connection. */
    public List<ValidationReport> validateAll(Connection pg, String schema, SnapshotManifest manifest,
                                              long eventPublisherId, int cursorBlocksToKeep)
            throws SQLException {
        return validateAll(pg, schema, manifest, eventPublisherId, cursorBlocksToKeep, null, 1, m -> { });
    }

    /**
     * Runs every offline level. Each loaded table is read once for all its per-row checks, on up to
     * {@code workers} connections from {@code connections}, with a status line every minute.
     */
    public List<ValidationReport> validateAll(Connection pg, String schema, SnapshotManifest manifest,
                                              long eventPublisherId, int cursorBlocksToKeep,
                                              DeferredIndexes.ConnectionFactory connections, int workers,
                                              Consumer<String> progress)
            throws SQLException {
        manifest = importView(pg, schema, manifest);
        Scans scans = new Scans(pg, schema, manifest, connections, workers, progress);
        List<ValidationReport> reports = new ArrayList<>();
        Progress.phase(progress, "Schema and load checks (one scan of each loaded table)");
        reports.add(validateLoad(pg, schema, manifest, scans));
        Progress.phase(progress, "Semantic checks");
        reports.add(validateSemantics(pg, schema, manifest, eventPublisherId, cursorBlocksToKeep, scans));
        return reports;
    }

    /**
     * The manifest as the import saw it: reduced by the retention the import added, which its journal
     * records, so the counts expected here are the ones the import loaded.
     */
    static SnapshotManifest importView(Connection pg, String schema, SnapshotManifest manifest) throws SQLException {
        ImportJournal journal = new ImportJournal(pg, schema);
        if (!journal.exists()) {
            return manifest;
        }
        String retention = journal.currentRun().map(ImportJournal.Run::retention).orElse(null);
        return Retention.apply(manifest, Retention.parse(retention));
    }
}
