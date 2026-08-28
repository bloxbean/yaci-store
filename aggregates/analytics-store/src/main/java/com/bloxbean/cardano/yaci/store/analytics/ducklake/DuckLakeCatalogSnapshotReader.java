package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Reads the committed state of the DuckLake catalog (tables and their data files) on behalf
 * of read-only consumers that must not attach the catalog themselves.
 *
 * <p><b>Why this exists.</b> DuckDB refuses to open the same database file from two DuckDB
 * instances in one process ({@code Unique file handle conflict}), regardless of READ_ONLY.
 * The DuckLake export writer keeps the catalog attached read-write on its single pooled
 * connection for the lifetime of the JVM, so any other in-memory DuckDB instance — for
 * example the sandboxed analytics query engine — can neither attach the DuckDB-file catalog
 * nor keep a persistent {@code ducklake_catalog} attachment without breaking exports.</p>
 *
 * <p>Instead, this component borrows the writer's own pooled connection (which already owns
 * the catalog attachment) to run two metadata queries: {@code duckdb_tables()} for the table
 * list and {@code ducklake_list_files()} for the data files of the current committed
 * snapshot. Consumers then read those Parquet files directly. Because only committed files
 * are listed, in-flight or aborted export files are never picked up.</p>
 *
 * <p>The writer pool has size 1, so a running export holds the connection. To avoid stalling
 * the caller (typically a scheduler thread) for the pool's connection timeout, a busy pool is
 * detected up front and {@link Optional#empty()} is returned; the caller keeps its previous
 * snapshot and retries later.</p>
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
public class DuckLakeCatalogSnapshotReader {

    static final String CATALOG_ALIAS = "ducklake_catalog";
    static final String CATALOG_SCHEMA = "main";

    private final DataSource writerDataSource;
    private final DuckDbConnectionHelper connectionHelper;
    private final AnalyticsStoreProperties properties;
    private final DuckLakeWriterLock writerLock;

    public DuckLakeCatalogSnapshotReader(
            @Qualifier("duckDbWriterDataSource") DataSource writerDataSource,
            DuckDbConnectionHelper connectionHelper,
            AnalyticsStoreProperties properties,
            DuckLakeWriterLock writerLock) {
        this.writerDataSource = writerDataSource;
        this.connectionHelper = connectionHelper;
        this.properties = properties;
        this.writerLock = writerLock;
    }

    /** Catalog column used to expose the schema of a table that has no data files yet. */
    public record Column(String name, String type) {
    }

    /**
     * Catalog snapshot of one DuckLake table.
     *
     * @param table          table name in the {@code main} schema
     * @param dataFiles      absolute, normalized paths of the committed Parquet data files
     * @param columns        columns in catalog order, including their DuckDB types
     * @param hasDeleteFiles true if the snapshot carries DuckLake delete files for this table
     *                       (never produced by the append-only exporter; reported for visibility)
     */
    public record TableFiles(String table, List<Path> dataFiles, List<Column> columns,
                             boolean hasDeleteFiles) {
    }

    /**
     * Read the committed catalog state.
     *
     * @return the tables of the catalog keyed by name (sorted), or empty when the writer
     *         connection is currently busy with an export and the caller should retry later
     * @throws SQLException if the catalog cannot be attached or queried
     */
    public Optional<Map<String, TableFiles>> readSnapshot() throws SQLException {
        Optional<DuckLakeWriterLock.Guard> guard = writerLock.tryAcquire();
        if (guard.isEmpty()) {
            log.debug("DuckLake writer connection is busy (export in progress); snapshot deferred");
            return Optional.empty();
        }

        try (DuckLakeWriterLock.Guard ignored = guard.get()) {
            if (isWriterBusy()) {
                log.debug("DuckLake writer connection is busy (export in progress); snapshot deferred");
                return Optional.empty();
            }
            try (Connection conn = writerDataSource.getConnection()) {
                // Idempotent on the writer's connection: the catalog is attached read-write once
                // (exactly what the export path and DuckLakeCatalogInitializer do) and reused.
                connectionHelper.prepareConnectionForDuckLake(conn, false, false);
                return Optional.of(readSnapshot(conn));
            }
        } catch (SQLTransientConnectionException e) {
            // Lost the race with an export that grabbed the single writer connection.
            log.debug("DuckLake writer connection unavailable ({}); snapshot deferred",
                    e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private Map<String, TableFiles> readSnapshot(Connection conn) throws SQLException {
        Path exportRoot = Paths.get(properties.getExportPath()).toAbsolutePath().normalize();
        Map<String, TableFiles> tables = new TreeMap<>();

        for (String table : listTables(conn)) {
            List<Path> files = new ArrayList<>();
            boolean hasDeleteFiles = false;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT data_file, delete_file FROM ducklake_list_files(?, ?)")) {
                stmt.setString(1, CATALOG_ALIAS);
                stmt.setString(2, table);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String dataFile = rs.getString(1);
                        if (dataFile == null || dataFile.isBlank()) continue;
                        // DuckLake stores paths relative to the catalog data_path exactly as it
                        // was configured (e.g. "./data/analytics/..."); resolve against the JVM
                        // working directory the writer used, so the query engine can address the
                        // file inside its allowed_directories sandbox.
                        Path resolved = Paths.get(dataFile).toAbsolutePath().normalize();
                        if (!resolved.startsWith(exportRoot)) {
                            log.warn("Skipping DuckLake data file outside the analytics export path "
                                    + "(table '{}', export path '{}')", table, exportRoot);
                            continue;
                        }
                        files.add(resolved);
                        if (rs.getString(2) != null) {
                            hasDeleteFiles = true;
                        }
                    }
                }
            }
            if (hasDeleteFiles) {
                log.warn("DuckLake table '{}' has delete files in the current snapshot; the analytics "
                        + "query layer reads data files directly and cannot apply them", table);
            }
            tables.put(table, new TableFiles(
                    table, List.copyOf(files), listColumns(conn, table), hasDeleteFiles));
        }
        return tables;
    }

    private static List<String> listTables(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT table_name FROM duckdb_tables() WHERE database_name = ? AND schema_name = ? "
                        + "ORDER BY table_name")) {
            stmt.setString(1, CATALOG_ALIAS);
            stmt.setString(2, CATALOG_SCHEMA);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
        }
        return tables;
    }

    private static List<Column> listColumns(Connection conn, String table) throws SQLException {
        List<Column> columns = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT column_name, data_type FROM duckdb_columns() "
                        + "WHERE database_name = ? AND schema_name = ? AND table_name = ? "
                        + "ORDER BY column_index")) {
            stmt.setString(1, CATALOG_ALIAS);
            stmt.setString(2, CATALOG_SCHEMA);
            stmt.setString(3, table);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    columns.add(new Column(rs.getString(1), rs.getString(2)));
                }
            }
        }
        return List.copyOf(columns);
    }

    /**
     * True when the single writer connection is checked out (an export is running). Uses the
     * pool's live statistics rather than waiting for the pool's connection timeout.
     */
    private boolean isWriterBusy() {
        if (!(writerDataSource instanceof HikariDataSource hikari)) {
            return false;
        }
        HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
        // Pool not started yet (first use) — nothing can be busy.
        return pool != null && pool.getActiveConnections() > 0 && pool.getIdleConnections() == 0;
    }
}
