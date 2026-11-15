package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.spec.ImportMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves one imported table's column plan against its PostgreSQL target before any data moves.
 *
 * <p>The same check runs at {@code snapshot inspect}/{@code export}, against the producing release's
 * schema, so a spec that cannot load is never packaged; at import preflight, so {@code --dry-run}
 * catches it; and once more over the extracted files just before the load. An SQL transform's output
 * types only exist once it is bound to inputs, so it is described over them: real files where they
 * are at hand, otherwise {@link #emptyRelation empty relations} of the recorded source types, which
 * also covers a table with no rows. Without this, a transform whose output did not fit its target
 * (mir: id VARCHAR into a uuid column) was found only after verification and extraction.
 */
public final class ImportPlanCheck {

    private ImportPlanCheck() {
    }

    /**
     * @param sourceColumns recorded source column name to DuckLake type
     * @param source        for SQL mode, the relation {@code ${files}} is bound to
     * @param dependencies  for SQL mode, dependency spec id to the relation {@code ${dep.<id>}} is bound to
     * @param target        the PostgreSQL target, or {@code null} to only prepare an SQL transform
     * @return the problems found; empty when the table can be loaded into {@code target}
     */
    public static List<String> check(Connection duck, ColumnPlanner planner, SnapshotTableSpec spec,
                                     Map<String, String> sourceColumns, String source,
                                     Map<String, String> dependencies, TargetTable target,
                                     long cutSlot, int completedEpoch) {
        try {
            if (spec.importSpec().mode() != ImportMode.SQL) {
                if (target != null) {
                    planner.plan(spec, sourceColumns, target);
                }
                return List.of();
            }
            String select = SqlResources.bindTransform(SqlResources.read(spec.importSpec().selectResource()),
                    source, dependencies, cutSlot, completedEpoch);
            Map<String, String> output = describe(duck, select);
            if (target != null) {
                planner.plan(spec, output, target);
            }
            return List.of();
        } catch (ColumnPlanner.MappingException e) {
            return List.of(e.getMessage());
        } catch (SQLException e) {
            String message = e.getMessage() == null ? "" : e.getMessage().replaceAll("\\s+", " ").trim();
            return List.of("SQL transform " + spec.importSpec().selectResource()
                    + " for '" + spec.id() + "' cannot be prepared: " + message);
        }
    }

    /** Output column name to DuckDB type of a SELECT, without running it. */
    public static Map<String, String> describe(Connection duck, String select) throws SQLException {
        Map<String, String> out = new LinkedHashMap<>();
        try (Statement st = duck.createStatement();
             ResultSet rs = st.executeQuery("DESCRIBE (" + select + ")")) {
            while (rs.next()) {
                out.put(rs.getString("column_name"), rs.getString("column_type"));
            }
        }
        return out;
    }

    /**
     * A relation with no rows and the given columns, standing in for files that are not at hand.
     *
     * @param columns column name to DuckLake type, as the catalog and the manifest record it
     */
    public static String emptyRelation(Map<String, String> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("An empty relation needs at least one column");
        }
        return "(SELECT " + columns.entrySet().stream()
                .map(c -> "CAST(NULL AS " + duckDbType(c.getValue()) + ") AS " + Identifiers.quote(c.getKey()))
                .collect(Collectors.joining(", ")) + " WHERE false)";
    }

    /**
     * DuckLake names its integer and floating point types by width; DuckDB does not, and reads
     * {@code int8} as the PostgreSQL alias for BIGINT. Every other DuckLake type name is also a
     * DuckDB one.
     */
    static String duckDbType(String ducklakeType) {
        String t = ducklakeType.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "int8" -> "TINYINT";
            case "int16" -> "SMALLINT";
            case "int32" -> "INTEGER";
            case "int64" -> "BIGINT";
            case "int128" -> "HUGEINT";
            case "uint8" -> "UTINYINT";
            case "uint16" -> "USMALLINT";
            case "uint32" -> "UINTEGER";
            case "uint64" -> "UBIGINT";
            case "uint128" -> "UHUGEINT";
            case "float32" -> "FLOAT";
            case "float64" -> "DOUBLE";
            default -> t;
        };
    }
}
