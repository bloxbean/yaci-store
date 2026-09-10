package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Builds unified DuckDB views that UNION ALL the exported (historical) data with live
 * PostgreSQL data.
 *
 * <p>For each table, introspects the historical view schema and the PostgreSQL table schema
 * (via the attached postgres_scanner database), maps renamed/derived columns, detects type
 * mismatches and generates the casts that make the UNION ALL work.</p>
 *
 * <p>The boundary between the two halves is applied to the exporter-declared
 * {@linkplain com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter#getFederationBoundaryColumn()
 * federation boundary column} — a slot column for DAILY tables (compared with the cutoff slot)
 * or the epoch column for EPOCH tables (compared with the cutoff epoch) — so that the split is
 * exact along the partition key. Columns that PostgreSQL names differently are resolved through
 * the exporter's
 * {@linkplain com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter#getSourceColumnMappings()
 * source column mappings}.</p>
 *
 * <p>Common type conversions handled automatically:</p>
 * <ul>
 *   <li>Parquet TIMESTAMP vs PG BIGINT (block_time) → {@code to_timestamp(col)}</li>
 *   <li>Parquet DECIMAL vs PG BIGINT (amounts) → {@code CAST(col AS DECIMAL(38,0))}</li>
 *   <li>Parquet VARCHAR vs PG JSON/JSONB → already mapped to VARCHAR by postgres_scanner</li>
 *   <li>Synthetic {@code date} partition column derived from the PG partition column</li>
 * </ul>
 */
@Slf4j
public class UnifiedViewBuilder {

    /**
     * PostgreSQL source table overrides.
     * Key = exported table name, Value = PostgreSQL table/view name.
     * Tables not in this map use the same name as the exported table.
     */
    private static final Map<String, String> PG_SOURCE_OVERRIDES = Map.of(
            "address_utxo", "address_utxo_flattened"
    );

    private UnifiedViewBuilder() {
    }

    /**
     * How a table is federated: which exported column carries the boundary, in which unit,
     * and how renamed columns are read from PostgreSQL. Derived from the table's exporter.
     *
     * @param boundaryColumn exported column compared with the cutoff; {@code null} = never federate
     * @param strategy       partition strategy — decides whether the cutoff slot (DAILY) or the
     *                       cutoff epoch (EPOCH) is applied to the boundary column
     * @param sourceColumns  exported column → PostgreSQL column name for renamed columns
     */
    public record Federation(String boundaryColumn, PartitionStrategy strategy, Map<String, String> sourceColumns) {
        public Federation {
            sourceColumns = sourceColumns == null ? Map.of() : Map.copyOf(sourceColumns);
        }
    }

    /**
     * Build the SQL to create a unified view for a table.
     *
     * @param tableName       the analytics table name (e.g., "block")
     * @param pgDatabaseAlias the attached PostgreSQL database alias (e.g., "pg_live")
     * @param pgSchema        the PostgreSQL schema (e.g., "preprod")
     * @param cutoff          the contiguous range served from exported data; rows outside it come from PG
     * @param federation      exporter-declared federation metadata for the table
     * @param partitionColumn source column used to derive DuckLake's synthetic date partition
     * @param parentConn      the DuckDB parent connection (for schema introspection)
     * @return the CREATE VIEW SQL, or null if the table cannot be federated
     */
    public static String buildUnifiedViewSql(
            String tableName,
            String pgDatabaseAlias,
            String pgSchema,
            CutoffSlotResolver.Cutoff cutoff,
            Federation federation,
            String partitionColumn,
            Connection parentConn) {

        if (federation == null || federation.boundaryColumn() == null) {
            log.debug("Table '{}' declares no federation boundary column, skipping federation", tableName);
            return null;
        }
        String boundaryColumn = federation.boundaryColumn();
        long rangeStart;
        long rangeEnd;
        if (federation.strategy() == PartitionStrategy.EPOCH) {
            rangeStart = cutoff.startEpoch();
            rangeEnd = cutoff.epoch();
        } else if (federation.strategy() == PartitionStrategy.DAILY) {
            rangeStart = cutoff.startSlot();
            rangeEnd = cutoff.slot();
        } else {
            log.debug("Table '{}' has partition strategy {}, skipping federation", tableName, federation.strategy());
            return null;
        }

        String parquetViewName = "parquet_" + tableName;
        String pgSourceTable = PG_SOURCE_OVERRIDES.getOrDefault(tableName, tableName);
        String pgFullName = quoteId(pgDatabaseAlias) + "." + quoteId(pgSchema) + "." + quoteId(pgSourceTable);

        try {
            // Introspect the historical view schema (target)
            List<ColumnInfo> parquetColumns = describeView(parentConn, quoteId(parquetViewName));
            if (parquetColumns.isEmpty()) {
                log.warn("No columns found for view '{}', skipping federation", parquetViewName);
                return null;
            }

            // The boundary column must exist in the exported schema
            boolean hasBoundary = parquetColumns.stream().anyMatch(c -> boundaryColumn.equals(c.name));
            if (!hasBoundary) {
                log.warn("Table '{}' has no '{}' column (declared federation boundary), skipping federation",
                        tableName, boundaryColumn);
                return null;
            }

            // Introspect PostgreSQL table schema (source)
            List<ColumnInfo> pgColumns = describeView(parentConn, pgFullName);
            if (pgColumns.isEmpty()) {
                log.warn("No columns found for PG source '{}', skipping federation for '{}'",
                        pgFullName, tableName);
                return null;
            }

            Map<String, String> pgColumnTypes = new LinkedHashMap<>();
            for (ColumnInfo col : pgColumns) {
                pgColumnTypes.put(col.name, col.type);
            }

            // Generate SELECT list for the PostgreSQL side with mappings and type conversions
            List<String> pgSelectColumns = new ArrayList<>();
            for (ColumnInfo parquetCol : parquetColumns) {
                String source = federation.sourceColumns().getOrDefault(parquetCol.name, parquetCol.name);
                String pgType = pgColumnTypes.get(source);
                if (pgType == null) {
                    if ("date".equals(parquetCol.name)) {
                        String dateExpression = buildDateExpression(
                                partitionColumn, parquetCol.type, pgColumnTypes);
                        if (dateExpression != null) {
                            pgSelectColumns.add(dateExpression);
                            continue;
                        }
                    }
                    log.info("Table '{}' cannot be federated: exported column '{}' (source '{}') not found in PG source {}",
                            tableName, parquetCol.name, source, pgFullName);
                    return null;
                }
                pgSelectColumns.add(buildColumnExpression(parquetCol.name, source, parquetCol.type, pgType));
            }

            // Boundary predicate on the PostgreSQL side uses the source column of the boundary
            String pgBoundary = quoteId(federation.sourceColumns().getOrDefault(boundaryColumn, boundaryColumn));
            String parquetPredicate = insideRange(quoteId(boundaryColumn), rangeStart, rangeEnd);
            String pgPredicate = outsideRange(pgBoundary, rangeStart, rangeEnd);

            // Build the UNION ALL view
            String pgSelect = String.join(", ", pgSelectColumns);
            return String.format(
                    "CREATE OR REPLACE VIEW %s AS " +
                    "SELECT * FROM %s WHERE %s " +
                    "UNION ALL " +
                    "SELECT %s FROM %s WHERE %s",
                    quoteId(tableName),
                    quoteId(parquetViewName),
                    parquetPredicate,
                    pgSelect,
                    pgFullName,
                    pgPredicate
            );

        } catch (SQLException e) {
            log.warn("Failed to build unified view for '{}': {}",
                    tableName, DuckDbConnectionHelper.redactSecrets(e.getMessage()));
            return null;
        }
    }

    private static String insideRange(String column, long start, long end) {
        if (end < start) return "FALSE";
        if (start == Long.MIN_VALUE) return column + " <= " + end;
        return column + " BETWEEN " + start + " AND " + end;
    }

    private static String outsideRange(String column, long start, long end) {
        if (end < start) return "TRUE";
        if (start == Long.MIN_VALUE) return column + " > " + end;
        return "(" + column + " < " + start + " OR " + column + " > " + end + ")";
    }

    private static String buildDateExpression(
            String partitionColumn, String parquetType, Map<String, String> pgColumnTypes) {
        if (partitionColumn == null || partitionColumn.isBlank()) return null;
        String pgType = pgColumnTypes.get(partitionColumn);
        if (pgType == null) return null;

        String normalizedPg = normalizeType(pgType);
        String source = quoteId(partitionColumn);
        String dateValue;
        if (isIntegerType(normalizedPg)) {
            dateValue = "CAST(to_timestamp(" + source + ") AT TIME ZONE 'UTC' AS DATE)";
        } else if (normalizedPg.startsWith("TIMESTAMP") || normalizedPg.equals("DATE")) {
            dateValue = "CAST(" + source + " AS DATE)";
        } else {
            return null;
        }
        return "CAST(" + dateValue + " AS " + parquetType + ") AS " + quoteId("date");
    }

    /**
     * Generate a column expression with type conversion if needed.
     */
    static String buildColumnExpression(String columnName, String parquetType, String pgType) {
        return buildColumnExpression(columnName, columnName, parquetType, pgType);
    }

    /**
     * Generate a column expression selecting PostgreSQL column {@code sourceColumn} as the
     * exported {@code columnName}, with type conversion if needed.
     */
    static String buildColumnExpression(String columnName, String sourceColumn, String parquetType, String pgType) {
        String normalizedParquet = normalizeType(parquetType);
        String normalizedPg = normalizeType(pgType);
        String source = quoteId(sourceColumn);
        String alias = " AS " + quoteId(columnName);

        // Types match — use column directly (aliased if renamed)
        if (normalizedParquet.equals(normalizedPg)) {
            return sourceColumn.equals(columnName) ? source : source + alias;
        }

        // BIGINT -> TIMESTAMP (common for block_time columns)
        if (normalizedParquet.startsWith("TIMESTAMP") && normalizedPg.equals("BIGINT")) {
            return "to_timestamp(" + source + ")" + alias;
        }

        // BIGINT -> DECIMAL (common for large amount columns)
        if (normalizedParquet.startsWith("DECIMAL") && normalizedPg.equals("BIGINT")) {
            return "CAST(" + source + " AS " + parquetType + ")" + alias;
        }

        // INTEGER -> BIGINT or vice versa (safe implicit cast in DuckDB for UNION ALL)
        if (isIntegerType(normalizedParquet) && isIntegerType(normalizedPg)) {
            return "CAST(" + source + " AS " + parquetType + ")" + alias;
        }

        // BOOLEAN type differences
        if (normalizedParquet.equals("BOOLEAN") && !normalizedPg.equals("BOOLEAN")) {
            return "CAST(" + source + " AS BOOLEAN)" + alias;
        }

        // Fallback: try explicit CAST to target type
        log.debug("Type mismatch for column '{}': parquet={}, pg={} — applying CAST",
                columnName, parquetType, pgType);
        return "CAST(" + source + " AS " + parquetType + ")" + alias;
    }

    /**
     * Describe a view/table and return column names and types.
     */
    static List<ColumnInfo> describeView(Connection conn, String viewName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE " + viewName)) {
            while (rs.next()) {
                columns.add(new ColumnInfo(
                        rs.getString("column_name"),
                        rs.getString("column_type")
                ));
            }
        }
        return columns;
    }

    private static String normalizeType(String type) {
        if (type == null) return "";
        return type.toUpperCase().trim();
    }

    private static boolean isIntegerType(String normalizedType) {
        return normalizedType.equals("BIGINT") || normalizedType.equals("INTEGER")
                || normalizedType.equals("SMALLINT") || normalizedType.equals("TINYINT")
                || normalizedType.equals("HUGEINT");
    }

    static String quoteId(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    record ColumnInfo(String name, String type) {
    }
}
