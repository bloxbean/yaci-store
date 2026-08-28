package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeCatalog;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeFile;
import com.bloxbean.cardano.yaci.store.snapshot.spec.CutoffType;
import com.bloxbean.cardano.yaci.store.snapshot.spec.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Resolves, for each table, the immutable file set and row count implied by the chosen consistency
 * point. Shared by {@code snapshot inspect} and {@code snapshot export} so what is reported is
 * exactly what would be packaged.
 */
public class ExportPlanner {

    private static final Logger log = LoggerFactory.getLogger(ExportPlanner.class);

    private final DuckLakeCatalog catalog;

    public ExportPlanner(DuckLakeCatalog catalog) {
        this.catalog = catalog;
    }

    public TablePlan plan(SnapshotTableSpec spec, long snapshotId, int completedEpoch, long cutSlot)
            throws SQLException {
        List<String> problems = new ArrayList<>();
        String relation = spec.relation();

        Map<String, String> sourceColumns = catalog.columns(relation, snapshotId);
        if (sourceColumns.isEmpty()) {
            problems.add("DuckLake relation '" + relation + "' does not exist at snapshot " + snapshotId);
            return new TablePlan(spec, List.of(), 0, false, Map.of(), Map.of(), "", problems);
        }

        problems.addAll(checkDeclaredColumns(spec, sourceColumns));

        List<DuckLakeFile> allFiles = catalog.files(relation, snapshotId);
        SnapshotTableSpec.CutoffRule cutoff = spec.consistency().cutoff();
        long cutValue = cutoffValue(cutoff, completedEpoch, cutSlot);

        List<DuckLakeFile> files;
        long rowCount;
        boolean exact;

        List<String> sourceKey = spec.validation().sourceKey();
        if (!sourceKey.isEmpty()) {
            // A transform that regroups rows produces fewer rows than it reads, so the count the
            // importer must reproduce is the number of distinct source keys, not the file row count.
            Set<String> beyond = cutoff.type() == CutoffType.NONE ? Set.of()
                    : catalog.filePathsEntirelyAbove(relation, cutoff.column(), cutValue, snapshotId);
            files = allFiles.stream().filter(f -> !beyond.contains(f.relativePath())).toList();
            rowCount = files.isEmpty() ? 0 : countDistinct(files, sourceKey, cutoff, cutValue);
            exact = true;
        } else if (cutoff.type() == CutoffType.NONE) {
            files = allFiles;
            rowCount = allFiles.stream().mapToLong(DuckLakeFile::rowCount).sum();
            exact = true;
        } else {
            Set<String> beyond = catalog.filePathsEntirelyAbove(relation, cutoff.column(), cutValue, snapshotId);
            files = allFiles.stream().filter(f -> !beyond.contains(f.relativePath())).toList();

            Map<String, Long> fullyBelow =
                    catalog.fileRowCountsEntirelyBelow(relation, cutoff.column(), cutValue, snapshotId);
            long counted = 0;
            List<DuckLakeFile> straddling = new ArrayList<>();
            for (DuckLakeFile f : files) {
                Long n = fullyBelow.get(f.relativePath());
                if (n != null) {
                    counted += n;
                } else {
                    straddling.add(f);
                }
            }
            // Only files the statistics cannot settle are read, which is normally one or two per table.
            long measured = straddling.isEmpty() ? 0 : countRows(straddling, cutoff.column(), cutValue);
            rowCount = counted + measured;
            exact = true;
            if (!straddling.isEmpty()) {
                log.debug("{}: measured {} rows across {} straddling file(s)",
                        spec.id(), measured, straddling.size());
            }
        }

        Map<String, String> bounds = observedBounds(spec, relation, snapshotId);

        return new TablePlan(spec, files, rowCount, exact, sourceColumns, bounds,
                columnFingerprint(sourceColumns), problems);
    }

    static long cutoffValue(SnapshotTableSpec.CutoffRule cutoff, int completedEpoch, long cutSlot) {
        return switch (cutoff.type()) {
            case NONE -> Long.MAX_VALUE;
            case SLOT_LTE -> cutSlot;
            case EPOCH_LTE -> completedEpoch;
            case EPOCH_LTE_OFFSET -> completedEpoch - cutoff.offset();
        };
    }

    private long countDistinct(List<DuckLakeFile> files, List<String> keyColumns,
                               SnapshotTableSpec.CutoffRule cutoff, long cut) throws SQLException {
        String key = keyColumns.stream().map(Identifiers::quote).reduce((a, b) -> a + ", " + b).orElseThrow();
        String where = cutoff.type() == CutoffType.NONE ? ""
                : " WHERE " + Identifiers.quote(cutoff.column()) + " <= " + cut;
        String sql = "SELECT count(*) FROM (SELECT DISTINCT " + key + " FROM read_parquet("
                + catalog.parquetList(files) + ")" + where + ")";
        try (Statement st = catalog.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private long countRows(List<DuckLakeFile> files, String column, long cut) throws SQLException {
        String sql = "SELECT count(*) FROM read_parquet(" + catalog.parquetList(files) + ") WHERE "
                + Identifiers.quote(column) + " <= " + cut;
        try (Statement st = catalog.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    /** Every column the spec names must exist in the export, or the spec has drifted from reality. */
    private List<String> checkDeclaredColumns(SnapshotTableSpec spec, Map<String, String> sourceColumns) {
        List<String> problems = new ArrayList<>();
        Set<String> present = sourceColumns.keySet();

        SnapshotTableSpec.Consistency c = spec.consistency();
        if (c.cutoff().column() != null && !present.contains(c.cutoff().column())) {
            problems.add("cutoff column '" + c.cutoff().column() + "' is not exported");
        }
        if (c.completedEpoch().column() != null && !present.contains(c.completedEpoch().column())) {
            problems.add("completed-epoch column '" + c.completedEpoch().column() + "' is not exported");
        }
        if (c.coverage().column() != null && !present.contains(c.coverage().column())) {
            problems.add("coverage column '" + c.coverage().column() + "' is not exported");
        }
        if (spec.source().partition().column() != null
                && spec.source().partition().strategy() != PartitionStrategy.NONE
                && !present.contains(spec.source().partition().column())) {
            problems.add("partition column '" + spec.source().partition().column() + "' is not exported");
        }
        for (String ignored : spec.importSpec().ignoreSourceColumns()) {
            if (!present.contains(ignored)) {
                problems.add("ignore-source-columns names '" + ignored + "' which is not exported");
            }
        }
        // SQL-mode transforms read the source through their own resource, so only the mapped-mode
        // column references are checked here; the transform's own columns are checked when prepared.
        if (spec.importSpec().mode() != com.bloxbean.cardano.yaci.store.snapshot.spec.ImportMode.SQL) {
            spec.importSpec().columns().forEach((target, mapping) -> {
                if (mapping.source() != null && !present.contains(mapping.source())) {
                    problems.add("import.columns." + target + " reads '" + mapping.source()
                            + "' which is not exported");
                }
            });
        }
        return problems;
    }

    private Map<String, String> observedBounds(SnapshotTableSpec spec, String relation, long snapshotId)
            throws SQLException {
        Map<String, String> out = new TreeMap<>();
        Set<String> columns = new TreeSet<>(spec.validation().bounds());
        for (String col : columns) {
            Optional<DuckLakeCatalog.Bounds> b = catalog.bounds(relation, col, snapshotId);
            b.ifPresent(v -> {
                out.put(col + ".min", v.min());
                out.put(col + ".max", v.max());
            });
        }
        return out;
    }

    /** Digest of the observed source column names and types, so schema drift is detectable. */
    public static String columnFingerprint(Map<String, String> columns) {
        StringBuilder sb = new StringBuilder();
        new LinkedHashMap<>(columns).forEach((k, v) -> sb.append(k).append(':').append(v).append(';'));
        return Digests.sha256Hex(sb.toString());
    }
}
