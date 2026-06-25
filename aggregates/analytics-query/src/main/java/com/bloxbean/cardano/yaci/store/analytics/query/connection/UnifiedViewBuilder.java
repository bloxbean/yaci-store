package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Builds unified DuckDB views that UNION ALL historical Parquet data with live PostgreSQL data.
 *
 * <p>For each table, introspects the Parquet view schema and the PostgreSQL table schema
 * (via the attached postgres_scanner database), detects type mismatches, and generates
 * appropriate casts to make the UNION ALL work.</p>
 *
 * <p>Common type conversions handled automatically:</p>
 * <ul>
 *   <li>Parquet TIMESTAMP vs PG BIGINT (block_time) → {@code to_timestamp(col)}</li>
 *   <li>Parquet DECIMAL vs PG BIGINT (amounts) → {@code CAST(col AS DECIMAL(38,0))}</li>
 *   <li>Parquet VARCHAR vs PG JSON/JSONB → already mapped to VARCHAR by postgres_scanner</li>
 * </ul>
 */
@Slf4j
public class UnifiedViewBuilder {

    /**
     * PostgreSQL source table overrides.
     * Key = Parquet table name, Value = PostgreSQL table/view name.
     * Tables not in this map use the same name as the Parquet table.
     */
    private static final Map<String, String> PG_SOURCE_OVERRIDES = Map.of(
            "address_utxo", "address_utxo_flattened"
    );

    /**
     * Tables that should never be federated (no slot column, reference data, etc.).
     */
    private static final Set<String> NEVER_FEDERATE = Set.of(
            "committee_member", "committee_state", "cost_model",
            "epoch_param", "unclaimed_reward_rest", "gov_epoch_activity"
    );

    private UnifiedViewBuilder() {
    }

    /**
     * Build the SQL to create a unified view for a table.
     *
     * @param tableName       the analytics table name (e.g., "block")
     * @param pgDatabaseAlias the attached PostgreSQL database alias (e.g., "pg_live")
     * @param pgSchema        the PostgreSQL schema (e.g., "preprod")
     * @param cutoffSlot      the boundary slot (data &lt;= cutoff from Parquet, &gt; cutoff from PG)
     * @param parentConn      the DuckDB parent connection (for schema introspection)
     * @return the CREATE VIEW SQL, or null if the table cannot be federated
     */
    public static String buildUnifiedViewSql(
            String tableName,
            String pgDatabaseAlias,
            String pgSchema,
            long cutoffSlot,
            Connection parentConn) {

        if (NEVER_FEDERATE.contains(tableName)) {
            log.debug("Table '{}' is in NEVER_FEDERATE list, skipping federation", tableName);
            return null;
        }

        String parquetViewName = "parquet_" + tableName;
        String pgSourceTable = PG_SOURCE_OVERRIDES.getOrDefault(tableName, tableName);
        String pgFullName = quoteId(pgDatabaseAlias) + "." + quoteId(pgSchema) + "." + quoteId(pgSourceTable);

        try {
            // Introspect Parquet view schema (target)
            List<ColumnInfo> parquetColumns = describeView(parentConn, quoteId(parquetViewName));
            if (parquetColumns.isEmpty()) {
                log.warn("No columns found for Parquet view '{}', skipping federation", parquetViewName);
                return null;
            }

            // Check that 'slot' column exists (needed for cutoff filter)
            boolean hasSlot = parquetColumns.stream().anyMatch(c -> "slot".equals(c.name));
            if (!hasSlot) {
                log.debug("Table '{}' has no 'slot' column, skipping federation", tableName);
                return null;
            }

            // Introspect PostgreSQL table schema (source)
            List<ColumnInfo> pgColumns = describeView(parentConn, pgFullName);
            if (pgColumns.isEmpty()) {
                log.warn("No columns found for PG source '{}', skipping federation for '{}'",
                        pgFullName, tableName);
                return null;
            }

            // Build column mapping
            Map<String, String> pgColumnTypes = new LinkedHashMap<>();
            for (ColumnInfo col : pgColumns) {
                pgColumnTypes.put(col.name, col.type);
            }

            // Generate SELECT list for the PostgreSQL side with type conversions
            List<String> pgSelectColumns = new ArrayList<>();
            boolean allColumnsFound = true;

            for (ColumnInfo parquetCol : parquetColumns) {
                String pgType = pgColumnTypes.get(parquetCol.name);
                if (pgType == null) {
                    log.debug("Column '{}' not found in PG source '{}' for table '{}'",
                            parquetCol.name, pgFullName, tableName);
                    allColumnsFound = false;
                    break;
                }

                String expr = buildColumnExpression(parquetCol.name, parquetCol.type, pgType);
                pgSelectColumns.add(expr);
            }

            if (!allColumnsFound) {
                log.info("Table '{}' cannot be federated: column mismatch with PG source", tableName);
                return null;
            }

            // Build the UNION ALL view
            String pgSelect = String.join(", ", pgSelectColumns);
            return String.format(
                    "CREATE OR REPLACE VIEW %s AS " +
                    "SELECT * FROM %s WHERE slot <= %d " +
                    "UNION ALL " +
                    "SELECT %s FROM %s WHERE slot > %d",
                    quoteId(tableName),
                    quoteId(parquetViewName),
                    cutoffSlot,
                    pgSelect,
                    pgFullName,
                    cutoffSlot
            );

        } catch (SQLException e) {
            log.warn("Failed to build unified view for '{}': {}", tableName, e.getMessage());
            return null;
        }
    }

    /**
     * Generate a column expression with type conversion if needed.
     */
    static String buildColumnExpression(String columnName, String parquetType, String pgType) {
        String normalizedParquet = normalizeType(parquetType);
        String normalizedPg = normalizeType(pgType);

        // Types match — use column directly
        if (normalizedParquet.equals(normalizedPg)) {
            return quoteId(columnName);
        }

        // BIGINT -> TIMESTAMP (common for block_time columns)
        if (normalizedParquet.startsWith("TIMESTAMP") && normalizedPg.equals("BIGINT")) {
            return "to_timestamp(" + quoteId(columnName) + ") AS " + quoteId(columnName);
        }

        // BIGINT -> DECIMAL (common for large amount columns)
        if (normalizedParquet.startsWith("DECIMAL") && normalizedPg.equals("BIGINT")) {
            return "CAST(" + quoteId(columnName) + " AS " + parquetType + ") AS " + quoteId(columnName);
        }

        // INTEGER -> BIGINT or vice versa (safe implicit cast in DuckDB for UNION ALL)
        if (isIntegerType(normalizedParquet) && isIntegerType(normalizedPg)) {
            return "CAST(" + quoteId(columnName) + " AS " + parquetType + ") AS " + quoteId(columnName);
        }

        // BOOLEAN type differences
        if (normalizedParquet.equals("BOOLEAN") && !normalizedPg.equals("BOOLEAN")) {
            return "CAST(" + quoteId(columnName) + " AS BOOLEAN) AS " + quoteId(columnName);
        }

        // Fallback: try explicit CAST to target type
        log.debug("Type mismatch for column '{}': parquet={}, pg={} — applying CAST",
                columnName, parquetType, pgType);
        return "CAST(" + quoteId(columnName) + " AS " + parquetType + ") AS " + quoteId(columnName);
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
