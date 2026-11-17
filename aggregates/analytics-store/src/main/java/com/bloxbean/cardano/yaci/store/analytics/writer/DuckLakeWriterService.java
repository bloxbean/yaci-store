package com.bloxbean.cardano.yaci.store.analytics.writer;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.ducklake.DuckLakeWriterLock;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.duckdb.DuckDBConnection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * DuckLake-based storage writer implementation.
 *
 * Exports data to Parquet files managed by DuckLake, providing:
 * - ACID transactions
 * - Time-travel queries
 * - Schema evolution
 * - Catalog metadata (PostgreSQL or DuckDB)
 *
 * DuckLake uses:
 * - Parquet files for data storage (same as ParquetWriterService)
 * - Catalog metadata for table management (PostgreSQL or DuckDB)
 * - DuckDB's ducklake extension for catalog operations
 *
 * Example output structure:
 * ./data/analytics/transactions/date=2024-01-15/data.parquet (data file)
 * ducklake_tables, ducklake_snapshots (catalog metadata in PostgreSQL/DuckDB)
 */
@Service("ducklakeWriter")
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
public class DuckLakeWriterService implements StorageWriter {

    static final String CATALOG = "ducklake_catalog";
    /** Database DuckLake attaches the catalog's metadata tables as. */
    static final String META = "__ducklake_metadata_" + CATALOG;

    private final DataSource duckDbDataSource;
    private final AnalyticsStoreProperties properties;
    private final DuckDbConnectionHelper connectionHelper;
    private final DuckLakeWriterLock writerLock;

    public DuckLakeWriterService(@Qualifier("duckDbWriterDataSource") DataSource duckDbDataSource,
                                  AnalyticsStoreProperties properties,
                                  DuckDbConnectionHelper connectionHelper,
                                  DuckLakeWriterLock writerLock) {
        this.duckDbDataSource = duckDbDataSource;
        this.properties = properties;
        this.connectionHelper = connectionHelper;
        this.writerLock = writerLock;

        log.info("Initialized DuckLakeWriterService (catalog: {}, source schema: {}, catalog URL: {})",
                properties.getDucklake().getCatalogType(),
                connectionHelper.getSourceCredentials().getSchema(),
                connectionHelper.getCatalogCredentials().getUrl().equals(connectionHelper.getSourceCredentials().getUrl()) ? "main datasource" : "custom");
    }

    @Override
    public ExportResult export(String query, String outputPath, String partitionColumn) {
        long startTime = System.currentTimeMillis();

        try (DuckLakeWriterLock.Guard ignored = writerLock.acquire();
             Connection conn = duckDbDataSource.getConnection()) {
            try {
                return export(conn, query, outputPath, startTime);
            } catch (SQLException e) {
                discardConnection(conn);
                throw e;
            }
        } catch (SQLException rawError) {
            // postgres_scanner / PostgreSQL-catalog errors echo the connection string
            // (including the password); log and propagate a redacted copy only.
            SQLException e = DuckDbConnectionHelper.sanitize(rawError);
            log.error("Failed to export to DuckLake: {}", e.getMessage(), e);
            throw new RuntimeException("DuckLake export failed: " + e.getMessage(), e);
        }
    }

    private ExportResult export(Connection conn, String query, String outputPath, long startTime) throws SQLException {
        // Prepare DuckDB connection for DuckLake (installs extensions, attaches catalog + source, sets search path)
        // readOnly=false for write operations
        connectionHelper.prepareConnectionForDuckLake(conn, true, false);

        // Extract table name from output path
        // Format: ./data/analytics/transactions/date=2024-01-15/data.parquet
        String tableName = extractTableNameFromPath(outputPath);
        Partition partition = partitionOf(outputPath);

        // Handle date partitioning query transformation
        String finalQuery = query;

        if (partition != null && partition.column().equals("date")) {
            // To get "date=yyyy-mm-dd" folder structure, we need an explicit "date" column.
            // It is the exported partition's date, not one derived from each row's timestamp:
            // the export selects rows by slot range, and a row without a block time
            // (COALESCE(block_time, 0)) would otherwise be filed under 1970-01-01.
            finalQuery = String.format("SELECT *, %s AS date FROM (%s)", partition.literal(), query);
        }

        Map<String, String> queryColumns = describe(conn, finalQuery);
        requirePartitionColumn(tableName, partition, queryColumns);

        if (!tableExists(conn, tableName)) {
            // Created and partitioned in one transaction, so a crash cannot leave the table
            // unpartitioned
            createPartitionedTable(conn, tableName, finalQuery, partition);
        } else {
            addMissingColumns(conn, tableName, queryColumns);
            // Repairs a table that an earlier release created without partitioning
            // (CREATE and ALTER ... SET PARTITIONED BY used to commit separately)
            ensurePartitioning(conn, tableName, partition);
        }

        // Replace the partition's rows (DELETE + INSERT in one DuckLake transaction)
        long rowCount = replacePartition(conn, tableName, partition, finalQuery);

        // Note: DuckLake manages its own files with UUID names in partition directories
        // The outputPath is used only for partition detection, not actual file placement
        // File size is not easily calculable as DuckLake may create multiple files
        long fileSize = 0;

        long duration = System.currentTimeMillis() - startTime;

        log.info("DuckLake export completed for table '{}': {} rows, {}ms",
                tableName, rowCount, duration);

        return new ExportResult(outputPath, rowCount, fileSize, duration);
    }

    /** Work done inside a transaction of {@link #inTransaction}. */
    @FunctionalInterface
    private interface TransactionWork<T> {
        T run(Statement stmt) throws SQLException;
    }

    /**
     * Runs {@code work} in one DuckDB transaction, opened and ended with explicit
     * {@code BEGIN TRANSACTION} / {@code COMMIT} / {@code ROLLBACK} statements while the JDBC
     * connection stays in auto-commit mode.
     *
     * <p>Deliberately not {@code setAutoCommit(false)} / {@code commit()} / {@code rollback()}:
     * the DuckDB JDBC driver keeps a flag of its own for "a transaction is open", sends
     * {@code BEGIN} only while that flag is clear, and clears it only when a COMMIT or ROLLBACK
     * succeeds. When the source PostgreSQL dies in the middle of an export, DuckDB ends the
     * transaction but its ROLLBACK reports an error (the postgres extension cannot roll back its
     * dead connection), so the flag stays set for the life of the connection. From then on the
     * driver never begins a transaction again: every statement commits on its own (a replace
     * deleted the partition and inserted the rows as two snapshots, and a failed insert left the
     * partition empty), and every commit fails with "cannot commit - no transaction is active".</p>
     *
     * <p>The first error is the one rethrown; a failed ROLLBACK is attached to it as suppressed.</p>
     */
    private static <T> T inTransaction(Connection conn, TransactionWork<T> work) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("BEGIN TRANSACTION");
            try {
                T result = work.run(stmt);
                stmt.execute("COMMIT");
                return result;
            } catch (SQLException | RuntimeException e) {
                try {
                    stmt.execute("ROLLBACK");
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        }
    }

    /**
     * Closes the physical DuckDB connection behind a pooled connection whose export failed, and
     * evicts it from the pool, so the next export starts on a new DuckDB instance that attaches
     * the catalog and the source again. A failure in the middle of a transaction can leave
     * state behind on the connection (an unfinished transaction, a broken source attachment)
     * that would otherwise fail, or silently change, every later export of this JVM.
     *
     * <p>The physical connection is closed here, before the pool replaces it: the pool closes
     * evicted connections asynchronously, and DuckDB lets only one instance per process have the
     * catalog file open, so a replacement could otherwise fail to attach it.</p>
     */
    private void discardConnection(Connection conn) {
        try {
            if (conn.isWrapperFor(DuckDBConnection.class)) {
                conn.unwrap(DuckDBConnection.class).close();
            }
        } catch (SQLException e) {
            log.warn("Failed to close the DuckDB writer connection after a failed export: {}",
                    DuckDbConnectionHelper.redactSecrets(e.getMessage()));
        }
        try {
            if (duckDbDataSource.isWrapperFor(HikariDataSource.class)) {
                duckDbDataSource.unwrap(HikariDataSource.class).evictConnection(conn);
            }
        } catch (SQLException e) {
            log.warn("Failed to evict the DuckDB writer connection after a failed export: {}",
                    DuckDbConnectionHelper.redactSecrets(e.getMessage()));
        }
        log.warn("Discarded the DuckDB writer connection after a failed export; "
                + "the next export opens a new one and attaches the catalog and source again");
    }

    @Override
    public String getStorageFormat() {
        return "DUCKLAKE";
    }

    @Override
    public String getSourceSchema() {
        String schema = connectionHelper.getSourceCredentials().getSchema();
        if (schema == null || schema.isEmpty()) {
            return schema;
        }
        return connectionHelper.quoteIdentifier(schema);
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM duckdb_tables() WHERE database_name = ? AND schema_name = 'main' "
                        + "AND table_name = ?")) {
            stmt.setString(1, CATALOG);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** Columns of a query, in order, with their DuckDB types. */
    private static Map<String, String> describe(Connection conn, String query) throws SQLException {
        Map<String, String> columns = new LinkedHashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE " + query)) {
            while (rs.next()) {
                columns.put(rs.getString("column_name"), rs.getString("column_type"));
            }
        }
        return columns;
    }

    /**
     * An epoch table is partitioned by its {@code epoch} column, and every replace deletes the
     * partition by it, so the export query must return that column. Without it the table could
     * not be partitioned, and every export would fail.
     */
    static void requirePartitionColumn(String tableName, Partition partition, Map<String, String> queryColumns) {
        if (partition == null) {
            return;
        }
        boolean present = queryColumns.keySet().stream().anyMatch(c -> c.equalsIgnoreCase(partition.column()));
        if (!present) {
            throw new IllegalArgumentException(String.format(
                    "The export query of table '%s' returns no '%s' column, so the table cannot be partitioned by it. "
                            + "Select it in the query (for a custom exporter alias the source column, "
                            + "e.g. r.earned_epoch AS %s). Query columns: %s",
                    tableName, partition.column(), partition.column(), queryColumns.keySet()));
        }
    }

    /**
     * Creates the table from the query's schema (CREATE TABLE AS ... LIMIT 0) and sets its
     * partitioning, in one transaction. DuckLake commits both as one snapshot, so the table
     * never exists without its partitioning.
     */
    private void createPartitionedTable(Connection conn, String tableName, String query, Partition partition)
            throws SQLException {
        inTransaction(conn, stmt -> {
            stmt.execute(String.format("CREATE TABLE %s AS %s LIMIT 0;", tableName, query));
            if (partition != null) {
                stmt.execute(String.format("ALTER TABLE %s SET PARTITIONED BY (%s);", tableName, partition.column()));
            }
            return null;
        });
        if (partition != null) {
            log.info("Created DuckLake table '{}' partitioned by {}", tableName, partition.column());
        } else {
            log.info("Created DuckLake table '{}' (no partition in the output path)", tableName);
        }
    }

    /**
     * Makes sure the table is partitioned by the exporter's partition key.
     *
     * <p>A table without partitioning (left by an interrupted create of an earlier release) is
     * partitioned now. Files written before that hold one partition value each (every export
     * writes a single partition), so a later replace still drops them whole. A file that holds
     * several values, or none, could only be replaced with a delete file, which the analytics
     * query layer cannot apply, so the export is refused for such a table and the operator
     * has to rebuild it.</p>
     *
     * <p>A table partitioned by something else is refused as well.</p>
     */
    private void ensurePartitioning(Connection conn, String tableName, Partition partition) throws SQLException {
        if (partition == null) {
            return;
        }
        List<String> keys = partitionKeys(conn, tableName);
        if (keys.equals(List.of(partition.column()))) {
            return;
        }
        if (!keys.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "DuckLake table '%s' is partitioned by %s, but its exporter writes '%s' partitions. "
                            + "Rebuild the table: drop it and reset its export state, then let it be exported again",
                    tableName, keys, partition.column()));
        }

        long mixedFiles = filesWithoutSingleValue(conn, tableName, partition.column());
        if (mixedFiles > 0) {
            throw new IllegalStateException(String.format(
                    "DuckLake table '%s' is not partitioned, and %d of its data files do not hold exactly one '%s' "
                            + "value, so a partition cannot be replaced without delete files. Rebuild the table: "
                            + "drop it and reset its export state, then let it be exported again",
                    tableName, mixedFiles, partition.column()));
        }

        connectionHelper.executeSql(conn,
                String.format("ALTER TABLE %s SET PARTITIONED BY (%s);", tableName, partition.column()));
        log.warn("DuckLake table '{}' had no partitioning (an earlier table creation was interrupted); "
                + "it is now partitioned by {}", tableName, partition.column());
    }

    /**
     * Current partition keys of a table, from the catalog metadata ({@code col} for an identity
     * transform, {@code transform(col)} otherwise). Empty when the table is not partitioned.
     */
    List<String> partitionKeys(Connection conn, String tableName) throws SQLException {
        List<String> keys = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT c.column_name, pc.transform FROM " + META + ".ducklake_table t "
                        + "JOIN " + META + ".ducklake_schema s ON s.schema_id = t.schema_id AND s.end_snapshot IS NULL "
                        + "JOIN " + META + ".ducklake_partition_info pi ON pi.table_id = t.table_id AND pi.end_snapshot IS NULL "
                        + "JOIN " + META + ".ducklake_partition_column pc ON pc.partition_id = pi.partition_id "
                        + "AND pc.table_id = t.table_id "
                        + "JOIN " + META + ".ducklake_column c ON c.table_id = t.table_id AND c.column_id = pc.column_id "
                        + "AND c.end_snapshot IS NULL "
                        + "WHERE t.end_snapshot IS NULL AND s.schema_name = 'main' AND t.table_name = ? "
                        + "ORDER BY pc.partition_key_index")) {
            stmt.setString(1, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String column = rs.getString(1);
                    String transform = rs.getString(2);
                    keys.add(transform == null || "identity".equalsIgnoreCase(transform)
                            ? column : transform + "(" + column + ")");
                }
            }
        }
        return keys;
    }

    /** Live data files of the table whose column statistics do not show exactly one non-NULL value of the column. */
    private static long filesWithoutSingleValue(Connection conn, String tableName, String column) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM " + META + ".ducklake_data_file df "
                        + "JOIN " + META + ".ducklake_table t ON t.table_id = df.table_id AND t.end_snapshot IS NULL "
                        + "JOIN " + META + ".ducklake_schema s ON s.schema_id = t.schema_id AND s.end_snapshot IS NULL "
                        + "LEFT JOIN " + META + ".ducklake_column c ON c.table_id = t.table_id AND c.column_name = ? "
                        + "AND c.end_snapshot IS NULL "
                        + "LEFT JOIN " + META + ".ducklake_file_column_stats st ON st.data_file_id = df.data_file_id "
                        + "AND st.column_id = c.column_id "
                        + "WHERE df.end_snapshot IS NULL AND s.schema_name = 'main' AND t.table_name = ? "
                        + "AND (st.min_value IS NULL OR st.max_value IS NULL OR st.min_value <> st.max_value "
                        + "OR COALESCE(st.null_count, 0) > 0)")) {
            stmt.setString(1, column);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /**
     * Adds the columns an exporter's query has gained since the table was created, so a release that
     * exports one more column keeps exporting into the existing table. Earlier data files read the
     * new column as NULL. A column the query no longer returns stays in the table, NULL from now on.
     */
    private void addMissingColumns(Connection conn, String tableName, Map<String, String> queryColumns)
            throws SQLException {
        Set<String> existing = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT column_name FROM duckdb_columns() WHERE database_name = ? AND schema_name = 'main' "
                        + "AND table_name = ?")) {
            stmt.setString(1, CATALOG);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    existing.add(rs.getString(1));
                }
            }
        }

        for (Map.Entry<String, String> column : queryColumns.entrySet()) {
            if (existing.contains(column.getKey())) {
                continue;
            }
            connectionHelper.executeSql(conn, String.format("ALTER TABLE %s ADD COLUMN %s %s;",
                    tableName, connectionHelper.quoteIdentifier(column.getKey()), column.getValue()));
            log.info("Added column '{}' ({}) to DuckLake table '{}'", column.getKey(), column.getValue(), tableName);
        }
    }

    /**
     * Export data using DuckLake INSERT, matching the query's columns to the table's by name.
     */
    private long exportWithDuckLake(Connection conn, String tableName, String query) throws SQLException {
        // Compression settings are configured globally at catalog level
        // See DuckLakeCatalogInitializer which calls configureDuckLakeCatalogSettings()
        // Settings are applied to all Parquet files written by DuckLake

        // Insert data into DuckLake table (we're already using ducklake_catalog database)
        String insertSql = String.format(
                "INSERT INTO %s BY NAME %s;",
                tableName,
                query
        );

        try (Statement stmt = conn.createStatement()) {
            int rowCount = stmt.executeUpdate(insertSql);
            log.debug("Inserted {} rows into DuckLake table '{}'", rowCount, tableName);
            return rowCount;
        }
    }

    /**
     * Replace a partition's rows: DELETE the partition, then INSERT the export, in one DuckLake
     * transaction. A re-export (after an export-state reset, a crash mid-export, or a repair)
     * therefore yields the same rows as the first export instead of appending a second copy.
     * The DELETE covers whole partitions, so DuckLake retires the old data files rather than
     * writing deletion files (which snapshot export refuses).
     */
    private long replacePartition(Connection conn, String tableName, Partition partition, String query)
            throws SQLException {
        if (partition == null) {
            return exportWithDuckLake(conn, tableName, query);
        }

        long[] deletedAndInserted = inTransaction(conn, stmt -> new long[]{
                stmt.executeUpdate(String.format("DELETE FROM %s WHERE %s;", tableName, partition.filter())),
                exportWithDuckLake(conn, tableName, query)});
        long deleted = deletedAndInserted[0];
        long inserted = deletedAndInserted[1];
        if (deleted > 0) {
            log.info("Replaced {} existing rows of DuckLake table '{}' partition {} with {} rows",
                    deleted, tableName, partition.filter(), inserted);
        }
        return inserted;
    }

    /**
     * Rows exported before the partition date was authoritative and without a block time
     * (to_timestamp(COALESCE(block_time, 0))) sit in the date=1970-01-01 partition. Once the days
     * holding their slots have been re-exported, those rows exist under their real day as well,
     * and the 1970-01-01 partition is a duplicate.
     *
     * This deletes the whole 1970-01-01 partition only if every row in it has a slot in
     * [startSlot, endSlot) — the slots of the days just re-exported. Otherwise nothing is
     * deleted and the result reports the slots found, so the caller can widen the range.
     *
     * @return what the partition held, and whether it was deleted
     */
    public EpochZeroPartition purgeEpochZeroPartition(String tableName, long startSlot, long endSlot) {
        try (DuckLakeWriterLock.Guard ignored = writerLock.acquire();
             Connection conn = duckDbDataSource.getConnection()) {
            try {
                return purgeEpochZeroPartition(conn, tableName, startSlot, endSlot);
            } catch (SQLException e) {
                discardConnection(conn);
                throw e;
            }
        } catch (SQLException rawError) {
            SQLException e = DuckDbConnectionHelper.sanitize(rawError);
            log.error("Failed to purge date=1970-01-01 of DuckLake table '{}': {}", tableName, e.getMessage(), e);
            throw new RuntimeException("DuckLake purge failed: " + e.getMessage(), e);
        }
    }

    private EpochZeroPartition purgeEpochZeroPartition(Connection conn, String tableName, long startSlot,
                                                       long endSlot) throws SQLException {
        connectionHelper.prepareConnectionForDuckLake(conn, false, false);

        String columnsSql = String.format(
                "SELECT COUNT(*) FROM duckdb_columns() WHERE database_name = 'ducklake_catalog' "
                        + "AND table_name = '%s' AND column_name IN ('date', 'slot');", tableName);
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(columnsSql)) {
            if (!rs.next() || rs.getInt(1) < 2) {
                return new EpochZeroPartition(0, null, null, false);
            }
        }

        return inTransaction(conn, stmt -> {
            long rows;
            long rowsWithSlot;
            Long minSlot;
            Long maxSlot;
            try (ResultSet rs = stmt.executeQuery(String.format(
                    "SELECT COUNT(*), COUNT(slot), MIN(slot), MAX(slot) FROM %s WHERE %s;",
                    tableName, EPOCH_ZERO_DATE.filter()))) {
                rs.next();
                rows = rs.getLong(1);
                rowsWithSlot = rs.getLong(2);
                minSlot = rs.getObject(3) != null ? rs.getLong(3) : null;
                maxSlot = rs.getObject(4) != null ? rs.getLong(4) : null;
            }

            boolean covered = rows > 0 && rowsWithSlot == rows
                    && minSlot >= startSlot && maxSlot < endSlot;
            if (covered) {
                stmt.executeUpdate(String.format("DELETE FROM %s WHERE %s;", tableName, EPOCH_ZERO_DATE.filter()));
                log.info("Deleted {} mis-filed rows (slots {}..{}) from DuckLake table '{}' partition date=1970-01-01",
                        rows, minSlot, maxSlot, tableName);
            } else if (rows > 0) {
                log.warn("DuckLake table '{}' has {} rows under date=1970-01-01 (slots {}..{}) outside the "
                        + "re-exported slots [{}, {}); keeping them", tableName, rows, minSlot, maxSlot,
                        startSlot, endSlot);
            }
            return new EpochZeroPartition(rows, minSlot, maxSlot, covered);
        });
    }

    /**
     * Rows a DuckLake table holds under date=1970-01-01.
     *
     * @param rowCount rows in the partition
     * @param minSlot  lowest slot among them, or null
     * @param maxSlot  highest slot among them, or null
     * @param deleted  whether the partition was deleted
     */
    public record EpochZeroPartition(long rowCount, Long minSlot, Long maxSlot, boolean deleted) {
    }

    private static final Partition EPOCH_ZERO_DATE = new Partition("date", "DATE '1970-01-01'");

    /** Partition column and its value as a SQL literal. */
    record Partition(String column, String literal) {
        String filter() {
            return column + " = " + literal;
        }
    }

    /**
     * Partition of an output path, from its Hive-style directory segment
     * (date=2024-01-15 or epoch=450), or null if the path has none.
     */
    static Partition partitionOf(String outputPath) {
        String segment = Paths.get(outputPath).getParent().getFileName().toString();
        if (segment.startsWith("date=")) {
            return new Partition("date", "DATE '" + LocalDate.parse(segment.substring("date=".length())) + "'");
        }
        if (segment.startsWith("epoch=")) {
            return new Partition("epoch", String.valueOf(Integer.parseInt(segment.substring("epoch=".length()))));
        }
        return null;
    }

    /**
     * Checks the export against the catalog: the partition must hold exactly the exported number
     * of rows in the latest committed snapshot. Data files on disk prove nothing — a replaced
     * partition keeps its retired files until maintenance deletes them, and a recreated or
     * restored catalog may not reference files that are still on disk.
     */
    @Override
    public boolean verifyExport(String outputPath, long expectedRowCount) {
        if (expectedRowCount == 0) {
            return true;
        }

        String tableName = extractTableNameFromPath(outputPath);
        Partition partition = partitionOf(outputPath);
        try (DuckLakeWriterLock.Guard ignored = writerLock.acquire();
             Connection conn = duckDbDataSource.getConnection()) {
            connectionHelper.prepareConnectionForDuckLake(conn, false, false);
            if (!tableExists(conn, tableName)) {
                log.warn("DuckLake table '{}' does not exist in the catalog; partition {} is not exported",
                        tableName, partition != null ? partition.filter() : outputPath);
                return false;
            }
            String sql = partition != null
                    ? String.format("SELECT COUNT(*) FROM %s WHERE %s;", tableName, partition.filter())
                    : String.format("SELECT COUNT(*) FROM %s;", tableName);
            long actual;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                rs.next();
                actual = rs.getLong(1);
            }
            boolean matches = partition != null ? actual == expectedRowCount : actual >= expectedRowCount;
            if (!matches) {
                log.warn("DuckLake table '{}' holds {} rows for {}, expected {}", tableName, actual,
                        partition != null ? partition.filter() : "the table", expectedRowCount);
            }
            return matches;
        } catch (SQLException rawError) {
            SQLException e = DuckDbConnectionHelper.sanitize(rawError);
            log.error("Failed to verify DuckLake export for {}: {}", outputPath, e.getMessage());
            return false;
        } catch (RuntimeException e) {
            log.error("Failed to verify DuckLake export for {}: {}", outputPath, e.getMessage());
            return false;
        }
    }

    /**
     * Extract table name from output path.
     * Format: ./data/analytics/transactions/date=2024-01-15/data.parquet → "transactions"
     */
    static String extractTableNameFromPath(String outputPath) {
        Path path = Paths.get(outputPath);
        // Go up two levels: data.parquet → date=2024-01-15 → transactions
        Path tableDir = path.getParent().getParent();
        return tableDir.getFileName().toString();
    }
}
