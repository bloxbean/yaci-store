package com.bloxbean.cardano.yaci.store.snapshot.ducklake;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Read-only view over a DuckLake catalog.
 *
 * <p>The catalog is a plain DuckDB database whose {@code ducklake_*} tables are queried directly.
 * That avoids depending on the {@code ducklake} extension for packaging, keeps the export path
 * offline, and lets a snapshot pin one catalog version explicitly: every lookup here is evaluated
 * at a single {@code snapshotId}.
 *
 * <p>The catalog file itself is never written to. It is attached {@code READ_ONLY} and, because a
 * live analytics writer may hold the file lock, callers normally point this at a copy.
 */
public class DuckLakeCatalog implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DuckLakeCatalog.class);
    private static final String CAT = "snapcat";

    private final Connection conn;
    private final Path dataDir;
    private final boolean temporaryCatalogCopy;
    private final Path catalogPath;

    private DuckLakeCatalog(Connection conn, Path dataDir, Path catalogPath, boolean temporaryCatalogCopy) {
        this.conn = conn;
        this.dataDir = dataDir;
        this.catalogPath = catalogPath;
        this.temporaryCatalogCopy = temporaryCatalogCopy;
    }

    /**
     * @param dataDir     analytics data directory (the parent of {@code ducklake.catalog.db})
     * @param workDir     where a private catalog copy is placed; the original is treated as read-only
     * @param copyCatalog copy the catalog before attaching, so a running exporter's file lock and
     *                    ongoing writes cannot affect or be affected by the snapshot read
     */
    public static DuckLakeCatalog open(Path dataDir, Path workDir, boolean copyCatalog) throws SQLException, IOException {
        Path catalog = dataDir.resolve("ducklake.catalog.db");
        if (!Files.isRegularFile(catalog)) {
            throw new IllegalArgumentException("DuckLake catalog not found: " + catalog);
        }
        Path attachPath = catalog;
        boolean copied = false;
        if (copyCatalog) {
            Files.createDirectories(workDir);
            attachPath = workDir.resolve("ducklake.catalog.snapshot.db");
            Files.copy(catalog, attachPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            copied = true;
        }
        Connection conn = DuckDb.open("2GB", workDir, 0);
        DuckDb.exec(conn, "ATTACH '" + attachPath.toAbsolutePath().toString().replace("'", "''")
                + "' AS " + CAT + " (READ_ONLY)");
        log.debug("Attached DuckLake catalog {} (copy={})", attachPath, copied);
        return new DuckLakeCatalog(conn, dataDir, attachPath, copied);
    }

    public Path dataDir() {
        return dataDir;
    }

    public Connection connection() {
        return conn;
    }

    public String duckdbVersion() throws SQLException {
        return DuckDb.version(conn);
    }

    public Map<String, String> metadata() throws SQLException {
        Map<String, String> out = new TreeMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT key, value FROM " + CAT + ".ducklake_metadata WHERE scope IS NULL")) {
            while (rs.next()) {
                out.put(rs.getString(1), rs.getString(2));
            }
        }
        return out;
    }

    /** Newest catalog snapshot id; this is what an export pins. */
    public long latestSnapshotId() throws SQLException {
        return DuckDb.queryLong(conn, "SELECT max(snapshot_id) FROM " + CAT + ".ducklake_snapshot", -1);
    }

    /**
     * Deletion files break the append-only assumption of the initial snapshot format: a packaged
     * Parquet file would contain rows the catalog considers deleted. Export must refuse.
     */
    public long deleteFileCount(long snapshotId) throws SQLException {
        return DuckDb.queryLong(conn,
                "SELECT count(*) FROM " + CAT + ".ducklake_delete_file"
                        + " WHERE begin_snapshot <= " + snapshotId
                        + " AND (end_snapshot IS NULL OR end_snapshot > " + snapshotId + ")", 0);
    }

    public List<String> relations(long snapshotId) throws SQLException {
        List<String> out = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT table_name FROM " + CAT + ".ducklake_table"
                     + " WHERE begin_snapshot <= " + snapshotId
                     + " AND (end_snapshot IS NULL OR end_snapshot > " + snapshotId + ")"
                     + " ORDER BY table_name")) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        }
        return out;
    }

    /** Column name to DuckLake type, in declared order. */
    public Map<String, String> columns(String relation, long snapshotId) throws SQLException {
        Identifiers.requireSqlIdentifier(relation, "ducklake relation");
        Map<String, String> out = new LinkedHashMap<>();
        String sql = "SELECT c.column_name, c.column_type FROM " + CAT + ".ducklake_column c"
                + " JOIN " + CAT + ".ducklake_table t ON t.table_id = c.table_id"
                + " WHERE t.table_name = " + Identifiers.literal(relation)
                + "   AND t.begin_snapshot <= " + snapshotId
                + "   AND (t.end_snapshot IS NULL OR t.end_snapshot > " + snapshotId + ")"
                + "   AND c.begin_snapshot <= " + snapshotId
                + "   AND (c.end_snapshot IS NULL OR c.end_snapshot > " + snapshotId + ")"
                + " ORDER BY c.column_order";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.put(rs.getString(1), rs.getString(2));
            }
        }
        return out;
    }

    /** Immutable file set of a relation at the pinned snapshot, ordered for deterministic batching. */
    public List<DuckLakeFile> files(String relation, long snapshotId) throws SQLException {
        Identifiers.requireSqlIdentifier(relation, "ducklake relation");
        List<DuckLakeFile> out = new ArrayList<>();
        String sql = "SELECT f.path, f.path_is_relative, f.record_count, f.file_size_bytes"
                + " FROM " + CAT + ".ducklake_data_file f"
                + " JOIN " + CAT + ".ducklake_table t ON t.table_id = f.table_id"
                + " WHERE t.table_name = " + Identifiers.literal(relation)
                + "   AND t.begin_snapshot <= " + snapshotId
                + "   AND (t.end_snapshot IS NULL OR t.end_snapshot > " + snapshotId + ")"
                + "   AND f.begin_snapshot <= " + snapshotId
                + "   AND (f.end_snapshot IS NULL OR f.end_snapshot > " + snapshotId + ")"
                + " ORDER BY f.path";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String path = rs.getString(1);
                boolean relative = rs.getBoolean(2);
                String rel = relative ? path : dataDir.toAbsolutePath().relativize(Path.of(path)).toString();
                out.add(new DuckLakeFile(rel, rs.getLong(3), rs.getLong(4)));
            }
        }
        return out;
    }

    /** Aggregated min/max of one column across the pinned file set, read from catalog statistics. */
    public Optional<Bounds> bounds(String relation, String column, long snapshotId) throws SQLException {
        Identifiers.requireSqlIdentifier(relation, "ducklake relation");
        Identifiers.requireSqlIdentifier(column, "ducklake column");
        String sql = "SELECT min(s.min_value), max(s.max_value) FROM " + CAT + ".ducklake_file_column_stats s"
                + " JOIN " + CAT + ".ducklake_table t ON t.table_id = s.table_id"
                + " JOIN " + CAT + ".ducklake_column c ON c.table_id = s.table_id AND c.column_id = s.column_id"
                + " JOIN " + CAT + ".ducklake_data_file f ON f.data_file_id = s.data_file_id"
                + " WHERE t.table_name = " + Identifiers.literal(relation)
                + "   AND c.column_name = " + Identifiers.literal(column)
                + "   AND t.begin_snapshot <= " + snapshotId
                + "   AND (t.end_snapshot IS NULL OR t.end_snapshot > " + snapshotId + ")"
                + "   AND c.begin_snapshot <= " + snapshotId
                + "   AND (c.end_snapshot IS NULL OR c.end_snapshot > " + snapshotId + ")"
                + "   AND f.begin_snapshot <= " + snapshotId
                + "   AND (f.end_snapshot IS NULL OR f.end_snapshot > " + snapshotId + ")";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                String min = rs.getString(1);
                String max = rs.getString(2);
                if (min == null && max == null) {
                    return Optional.empty();
                }
                return Optional.of(new Bounds(min, max));
            }
        }
        return Optional.empty();
    }

    /** Numeric bounds, computed with a numeric cast so string ordering cannot mislead. */
    public Optional<long[]> numericBounds(String relation, String column, long snapshotId) throws SQLException {
        Identifiers.requireSqlIdentifier(relation, "ducklake relation");
        Identifiers.requireSqlIdentifier(column, "ducklake column");
        String sql = "SELECT min(TRY_CAST(s.min_value AS HUGEINT)), max(TRY_CAST(s.max_value AS HUGEINT))"
                + " FROM " + CAT + ".ducklake_file_column_stats s"
                + " JOIN " + CAT + ".ducklake_table t ON t.table_id = s.table_id"
                + " JOIN " + CAT + ".ducklake_column c ON c.table_id = s.table_id AND c.column_id = s.column_id"
                + " JOIN " + CAT + ".ducklake_data_file f ON f.data_file_id = s.data_file_id"
                + " WHERE t.table_name = " + Identifiers.literal(relation)
                + "   AND c.column_name = " + Identifiers.literal(column)
                + "   AND t.begin_snapshot <= " + snapshotId
                + "   AND (t.end_snapshot IS NULL OR t.end_snapshot > " + snapshotId + ")"
                + "   AND c.begin_snapshot <= " + snapshotId
                + "   AND (c.end_snapshot IS NULL OR c.end_snapshot > " + snapshotId + ")"
                + "   AND f.begin_snapshot <= " + snapshotId
                + "   AND (f.end_snapshot IS NULL OR f.end_snapshot > " + snapshotId + ")";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                long min = rs.getLong(1);
                boolean minNull = rs.wasNull();
                long max = rs.getLong(2);
                boolean maxNull = rs.wasNull();
                if (minNull || maxNull) {
                    return Optional.empty();
                }
                return Optional.of(new long[]{min, max});
            }
        }
        return Optional.empty();
    }


    /**
     * Files whose statistics say {@code column} may contain a value inside {@code [min,max]}.
     *
     * <p>Used to avoid scanning the whole relation when only one epoch is of interest: the catalog
     * already stores per-file min/max, so the point selection reads a handful of Parquet files
     * rather than every file of the table.
     */
    public List<DuckLakeFile> filesOverlapping(String relation, String column, long min, long max, long snapshotId)
            throws SQLException {
        Identifiers.requireSqlIdentifier(relation, "ducklake relation");
        Identifiers.requireSqlIdentifier(column, "ducklake column");
        List<DuckLakeFile> out = new ArrayList<>();
        String sql = "SELECT f.path, f.path_is_relative, f.record_count, f.file_size_bytes"
                + " FROM " + CAT + ".ducklake_data_file f"
                + " JOIN " + CAT + ".ducklake_table t ON t.table_id = f.table_id"
                + " JOIN " + CAT + ".ducklake_file_column_stats s ON s.data_file_id = f.data_file_id"
                + " JOIN " + CAT + ".ducklake_column c ON c.table_id = f.table_id AND c.column_id = s.column_id"
                + " WHERE t.table_name = " + Identifiers.literal(relation)
                + "   AND c.column_name = " + Identifiers.literal(column)
                + "   AND t.begin_snapshot <= " + snapshotId
                + "   AND (t.end_snapshot IS NULL OR t.end_snapshot > " + snapshotId + ")"
                + "   AND c.begin_snapshot <= " + snapshotId
                + "   AND (c.end_snapshot IS NULL OR c.end_snapshot > " + snapshotId + ")"
                + "   AND f.begin_snapshot <= " + snapshotId
                + "   AND (f.end_snapshot IS NULL OR f.end_snapshot > " + snapshotId + ")"
                + "   AND TRY_CAST(s.min_value AS HUGEINT) <= " + max
                + "   AND TRY_CAST(s.max_value AS HUGEINT) >= " + min
                + " ORDER BY f.path";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String path = rs.getString(1);
                boolean relative = rs.getBoolean(2);
                String rel = relative ? path : dataDir.toAbsolutePath().relativize(Path.of(path)).toString();
                out.add(new DuckLakeFile(rel, rs.getLong(3), rs.getLong(4)));
            }
        }
        return out;
    }

    /** Absolute paths for {@code read_parquet}, as a SQL list literal. */
    public String parquetList(List<DuckLakeFile> files) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < files.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(Identifiers.literal(dataDir.resolve(files.get(i).relativePath()).toAbsolutePath().toString()));
        }
        return sb.append(']').toString();
    }


    /**
     * Paths of files whose statistics prove every row is beyond {@code cut} on {@code column}.
     *
     * <p>Callers subtract this from the full file set rather than selecting overlapping files
     * directly, so a file that happens to have no statistics is kept instead of silently dropped.
     */
    public java.util.Set<String> filePathsEntirelyAbove(String relation, String column, long cut, long snapshotId)
            throws SQLException {
        Identifiers.requireSqlIdentifier(relation, "ducklake relation");
        Identifiers.requireSqlIdentifier(column, "ducklake column");
        java.util.Set<String> out = new java.util.HashSet<>();
        String sql = "SELECT f.path, f.path_is_relative"
                + " FROM " + CAT + ".ducklake_data_file f"
                + " JOIN " + CAT + ".ducklake_table t ON t.table_id = f.table_id"
                + " JOIN " + CAT + ".ducklake_file_column_stats s ON s.data_file_id = f.data_file_id"
                + " JOIN " + CAT + ".ducklake_column c ON c.table_id = f.table_id AND c.column_id = s.column_id"
                + " WHERE t.table_name = " + Identifiers.literal(relation)
                + "   AND c.column_name = " + Identifiers.literal(column)
                + "   AND t.begin_snapshot <= " + snapshotId
                + "   AND (t.end_snapshot IS NULL OR t.end_snapshot > " + snapshotId + ")"
                + "   AND c.begin_snapshot <= " + snapshotId
                + "   AND (c.end_snapshot IS NULL OR c.end_snapshot > " + snapshotId + ")"
                + "   AND f.begin_snapshot <= " + snapshotId
                + "   AND (f.end_snapshot IS NULL OR f.end_snapshot > " + snapshotId + ")"
                + "   AND TRY_CAST(s.min_value AS HUGEINT) > " + cut;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String path = rs.getString(1);
                boolean relative = rs.getBoolean(2);
                out.add(relative ? path : dataDir.toAbsolutePath().relativize(Path.of(path)).toString());
            }
        }
        return out;
    }

    /**
     * Paths of files whose statistics prove every row is at or below {@code cut}: their whole
     * {@code record_count} can be trusted without reading the file.
     */
    public Map<String, Long> fileRowCountsEntirelyBelow(String relation, String column, long cut, long snapshotId)
            throws SQLException {
        Identifiers.requireSqlIdentifier(relation, "ducklake relation");
        Identifiers.requireSqlIdentifier(column, "ducklake column");
        Map<String, Long> out = new java.util.HashMap<>();
        String sql = "SELECT f.path, f.path_is_relative, f.record_count"
                + " FROM " + CAT + ".ducklake_data_file f"
                + " JOIN " + CAT + ".ducklake_table t ON t.table_id = f.table_id"
                + " JOIN " + CAT + ".ducklake_file_column_stats s ON s.data_file_id = f.data_file_id"
                + " JOIN " + CAT + ".ducklake_column c ON c.table_id = f.table_id AND c.column_id = s.column_id"
                + " WHERE t.table_name = " + Identifiers.literal(relation)
                + "   AND c.column_name = " + Identifiers.literal(column)
                + "   AND t.begin_snapshot <= " + snapshotId
                + "   AND (t.end_snapshot IS NULL OR t.end_snapshot > " + snapshotId + ")"
                + "   AND c.begin_snapshot <= " + snapshotId
                + "   AND (c.end_snapshot IS NULL OR c.end_snapshot > " + snapshotId + ")"
                + "   AND f.begin_snapshot <= " + snapshotId
                + "   AND (f.end_snapshot IS NULL OR f.end_snapshot > " + snapshotId + ")"
                + "   AND TRY_CAST(s.max_value AS HUGEINT) <= " + cut
                + "   AND s.null_count = 0";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String path = rs.getString(1);
                boolean relative = rs.getBoolean(2);
                out.put(relative ? path : dataDir.toAbsolutePath().relativize(Path.of(path)).toString(),
                        rs.getLong(3));
            }
        }
        return out;
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            log.debug("Error closing catalog connection", e);
        }
        if (temporaryCatalogCopy) {
            try {
                Files.deleteIfExists(catalogPath);
            } catch (IOException e) {
                log.debug("Could not remove temporary catalog copy {}", catalogPath, e);
            }
        }
    }

    public record Bounds(String min, String max) {}
}
