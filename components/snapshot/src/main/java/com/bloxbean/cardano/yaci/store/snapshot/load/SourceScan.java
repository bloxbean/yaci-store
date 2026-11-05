package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckDb;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A scan of exported files that has every column its table declares.
 *
 * <p>When an exporter gains a column, DuckLake adds it to the relation and keeps the files written
 * before as they are. {@link DuckDb#readParquet} matches columns by name, so a scan that mixes older
 * and newer files reads the column as NULL for the older ones. A scan of older files only has no such
 * column at all, and a transform or mapping that names it would not bind; this adds it as a typed NULL.
 */
public final class SourceScan {

    private SourceScan() {
    }

    /**
     * @param duck     a DuckDB connection, used to read the files' schemas (footers only)
     * @param files    a DuckDB list literal of file paths
     * @param declared column name to DuckLake type, as the catalog and the manifest record it; null or
     *                 empty for a plain scan
     */
    public static String of(Connection duck, String files, Map<String, String> declared) throws SQLException {
        String scan = DuckDb.readParquet(files);
        if (duck == null || declared == null || declared.isEmpty()) {
            return scan;
        }
        Set<String> present = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (Statement st = duck.createStatement(); ResultSet rs = st.executeQuery("DESCRIBE SELECT * FROM " + scan)) {
            while (rs.next()) {
                present.add(rs.getString("column_name"));
            }
        }
        List<String> missing = new ArrayList<>();
        declared.forEach((column, type) -> {
            if (!present.contains(column)) {
                missing.add("CAST(NULL AS " + ImportPlanCheck.duckDbType(type) + ") AS " + Identifiers.quote(column));
            }
        });
        return missing.isEmpty() ? scan : "(SELECT *, " + String.join(", ", missing) + " FROM " + scan + ")";
    }
}
