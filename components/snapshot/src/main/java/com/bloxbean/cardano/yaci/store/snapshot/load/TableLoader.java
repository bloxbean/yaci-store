package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.convert.ConverterRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.CutoffType;
import com.bloxbean.cardano.yaci.store.snapshot.spec.ImportMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Executes one batch: build the DuckDB SELECT for the declared import mode, insert it into the
 * PostgreSQL target, and record the batch in the journal — all inside one transaction.
 *
 * <p>The same generic path serves DIRECT, MAPPED and SQL. Adding an ordinary table needs a YAML
 * specification, not a new loader class.
 */
public class TableLoader {

    private static final Logger log = LoggerFactory.getLogger(TableLoader.class);

    private final ConverterRegistry converters;
    private final ColumnPlanner planner;
    private final Path extractRoot;

    public TableLoader(ConverterRegistry converters, Path extractRoot) {
        this.converters = converters;
        this.planner = new ColumnPlanner(converters);
        this.extractRoot = extractRoot;
    }

    /** The SELECT a batch reads from, before the target column list is applied. */
    public String sourceSelect(SnapshotTableSpec spec, ImportBatch batch, long cutSlot, int completedEpoch,
                               Map<String, List<SnapshotManifest.FileEntry>> dependencyFiles) {
        if (spec.importSpec().mode() == ImportMode.SQL) {
            String sql = SqlResources.read(spec.importSpec().selectResource());
            Map<String, String> params = new LinkedHashMap<>();
            params.put("files", parquetList(batch.files()));
            params.put("cutSlot", Long.toString(cutSlot));
            params.put("completedEpoch", Integer.toString(completedEpoch));
            params.put("uuidNamespace", Identifiers.literal(ConverterRegistry.UUID_NAMESPACE));
            dependencyFiles.forEach((dep, files) -> params.put("dep." + dep, parquetList(files)));
            return "(" + SqlResources.bind(sql, params) + ")";
        }
        return "(SELECT * FROM read_parquet(" + parquetList(batch.files()) + ")"
                + cutoffPredicate(spec, cutSlot, completedEpoch) + ")";
    }

    static String cutoffPredicate(SnapshotTableSpec spec, long cutSlot, int completedEpoch) {
        SnapshotTableSpec.CutoffRule cutoff = spec.consistency().cutoff();
        if (cutoff.type() == CutoffType.NONE) {
            return "";
        }
        String col = Identifiers.quote(cutoff.column());
        long value = switch (cutoff.type()) {
            case SLOT_LTE -> cutSlot;
            case EPOCH_LTE -> completedEpoch;
            case EPOCH_LTE_OFFSET -> completedEpoch - cutoff.offset();
            case NONE -> Long.MAX_VALUE;
        };
        return " WHERE " + col + " <= " + value;
    }

    /**
     * Resolve the column plan against the real source and target schemas for this batch.
     *
     * <p>For DIRECT and MAPPED the declared types come from the manifest, which records what the
     * producing DuckLake catalog said. DuckDB widens INT32 to BIGINT when it reads Parquet, so
     * planning from its view would make every {@code int32 -> integer} column look like an unsafe
     * narrowing. The column <em>names</em> are still taken from the files, so a file whose schema has
     * drifted from the manifest still fails.
     *
     * <p>For SQL mode the transform's own output types are authoritative, because the transform casts
     * explicitly.
     */
    public ColumnPlan planFor(DuckPgSession session, SnapshotTableSpec spec, String select,
                              TargetTable target, Map<String, String> declaredSourceColumns)
            throws SQLException {
        Map<String, String> observed = session.describe(select);
        if (spec.importSpec().mode() == ImportMode.SQL
                || declaredSourceColumns == null || declaredSourceColumns.isEmpty()) {
            return planner.plan(spec, observed, target);
        }
        if (!observed.keySet().equals(declaredSourceColumns.keySet())) {
            Set<String> extra = new TreeSet<>(observed.keySet());
            extra.removeAll(declaredSourceColumns.keySet());
            Set<String> missing = new TreeSet<>(declaredSourceColumns.keySet());
            missing.removeAll(observed.keySet());
            throw new ColumnPlanner.MappingException("Packaged Parquet for '" + spec.id()
                    + "' does not match the manifest: unexpected column(s) " + extra
                    + ", missing column(s) " + missing);
        }
        return planner.plan(spec, declaredSourceColumns, target);
    }

    /**
     * Insert the batch and record it, atomically.
     *
     * @return rows inserted
     */
    public long loadBatch(DuckPgSession session, ImportBatch batch, ColumnPlan plan, String select,
                          String snapshotId, String schema) throws SQLException {
        String target = session.qualify(batch.spec().targetTable());
        String journal = ImportJournal.qualifiedBatchTable(DuckPgSession.PG_ALIAS, schema);

        String insert = "INSERT INTO " + target + " (" + plan.targetColumnList() + ") "
                + "SELECT " + plan.selectList() + " FROM " + select + " AS src";

        long rows;
        session.exec("BEGIN TRANSACTION");
        try {
            try (Statement st = session.connection().createStatement()) {
                rows = st.executeLargeUpdate(insert);
            }
            String record = "INSERT INTO " + journal
                    + " (snapshot_id, batch_id, spec_id, target_table, row_count) VALUES ("
                    + Identifiers.literal(snapshotId) + ", "
                    + Identifiers.literal(batch.batchId()) + ", "
                    + Identifiers.literal(batch.spec().id()) + ", "
                    + Identifiers.literal(batch.spec().targetTable()) + ", "
                    + rows + ")";
            session.exec(record);
            session.exec("COMMIT");
        } catch (SQLException e) {
            try {
                session.exec("ROLLBACK");
            } catch (SQLException ignored) {
                // The transaction is already gone; the original failure is the useful one.
            }
            throw e;
        }
        log.debug("Loaded batch {} of {} ({} rows)", batch.batchId().substring(0, 12),
                batch.spec().id(), rows);
        return rows;
    }

    /** Absolute paths of a batch's extracted files, as a DuckDB list literal. */
    public String parquetList(List<SnapshotManifest.FileEntry> files) {
        List<String> paths = new ArrayList<>();
        for (SnapshotManifest.FileEntry f : files) {
            paths.add(Identifiers.literal(extractRoot.resolve(f.path()).toAbsolutePath().toString()));
        }
        return "[" + String.join(", ", paths) + "]";
    }

    /** Rows currently in a target table, used by validation. */
    public static long count(DuckPgSession session, String table) throws SQLException {
        try (Statement st = session.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM " + session.qualify(table))) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }
}
