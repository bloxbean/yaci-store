package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises the reader against a real DuckLake catalog owned by a writer pool that mirrors
 * {@code DuckDbDataSourceConfig#duckDbWriterDataSource()} (in-memory DuckDB, pool size 1).
 * Skipped when the DuckLake extension cannot be loaded (no network / not cached).
 */
class DuckLakeCatalogSnapshotReaderTest {

    /** Relative export path, like the shipped configuration ({@code ./data/analytics}). */
    private Path relativeExportPath;
    private HikariDataSource writerDataSource;
    private DuckDbConnectionHelper helper;
    private AnalyticsStoreProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(duckLakeAvailable(), "DuckLake extension not available");

        relativeExportPath = Path.of("build", "tmp", "ducklake-snapshot-" + UUID.randomUUID(), "data", "analytics");
        Files.createDirectories(relativeExportPath);

        properties = new AnalyticsStoreProperties();
        properties.setExportPath("./" + relativeExportPath);
        properties.getStorage().setType("ducklake");
        properties.getDucklake().setCatalogType("duckdb");
        properties.getDucklake().setCatalogPath("./" + relativeExportPath.resolve("ducklake.catalog.db"));

        writerDataSource = new HikariDataSource();
        writerDataSource.setJdbcUrl("jdbc:duckdb:");
        writerDataSource.setDriverClassName("org.duckdb.DuckDBDriver");
        writerDataSource.setMaximumPoolSize(1);
        writerDataSource.setMinimumIdle(1);
        writerDataSource.setConnectionTimeout(250);

        helper = new DuckDbConnectionHelper(environment(), properties);
        // Same catalog initialization as DuckLakeCatalogInitializer at startup (compression,
        // and data inlining off so every committed row lands in a Parquet data file).
        try (Connection conn = writerDataSource.getConnection()) {
            helper.prepareConnectionForDuckLake(conn, false, false);
            helper.configureDuckLakeCatalogSettings(conn);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        if (writerDataSource != null) {
            writerDataSource.close();
        }
        if (relativeExportPath != null) {
            Path root = relativeExportPath.getParent().getParent();
            if (Files.exists(root)) {
                try (Stream<Path> paths = Files.walk(root)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
                }
            }
        }
    }

    @Test
    void listsCommittedFilesAsAbsolutePathsWithoutAttachingElsewhere() throws Exception {
        writeTable("block", "SELECT range AS slot, CAST('2024-01-01' AS DATE) AS date FROM range(10)");
        appendRows("block", "SELECT range + 100 AS slot, CAST('2024-01-02' AS DATE) AS date FROM range(5)");
        writeTable("emptytbl", "SELECT 1::BIGINT AS slot LIMIT 0");

        DuckLakeCatalogSnapshotReader reader = new DuckLakeCatalogSnapshotReader(writerDataSource, helper, properties);
        Optional<Map<String, DuckLakeCatalogSnapshotReader.TableFiles>> snapshot = reader.readSnapshot();

        assertTrue(snapshot.isPresent());
        Map<String, DuckLakeCatalogSnapshotReader.TableFiles> tables = snapshot.get();
        assertEquals(2, tables.size());
        assertTrue(tables.get("emptytbl").dataFiles().isEmpty());

        DuckLakeCatalogSnapshotReader.TableFiles block = tables.get("block");
        assertEquals(2, block.dataFiles().size());
        assertFalse(block.hasDeleteFiles());
        Path absoluteExportRoot = relativeExportPath.toAbsolutePath().normalize();
        for (Path file : block.dataFiles()) {
            assertTrue(file.isAbsolute(), file.toString());
            assertTrue(file.startsWith(absoluteExportRoot), file.toString());
            assertTrue(Files.isRegularFile(file), file.toString());
        }

        // The writer still owns the catalog: it can keep exporting after the snapshot was read ...
        appendRows("block", "SELECT range + 200 AS slot, CAST('2024-01-03' AS DATE) AS date FROM range(5)");
        assertEquals(3, reader.readSnapshot().orElseThrow().get("block").dataFiles().size());

        // ... and, as documented, no other in-process DuckDB instance may attach the catalog file.
        try (Connection other = DriverManager.getConnection("jdbc:duckdb:");
             Statement stmt = other.createStatement()) {
            stmt.execute("INSTALL ducklake");
            stmt.execute("LOAD ducklake");
            SQLException conflict = assertThrows(SQLException.class, () -> stmt.execute(
                    "ATTACH 'ducklake:" + properties.getDucklake().getCatalogPath()
                            + "' AS ducklake_catalog (READ_ONLY, DATA_PATH '" + properties.getExportPath() + "')"));
            assertTrue(conflict.getMessage().contains("Unique file handle conflict"), conflict.getMessage());
        }
    }

    @Test
    void defersInsteadOfBlockingWhileAnExportHoldsTheWriterConnection() throws Exception {
        writeTable("block", "SELECT range AS slot FROM range(3)");
        DuckLakeCatalogSnapshotReader reader = new DuckLakeCatalogSnapshotReader(writerDataSource, helper, properties);
        assertTrue(reader.readSnapshot().isPresent());

        try (Connection heldByExport = writerDataSource.getConnection()) {
            long start = System.nanoTime();
            Optional<?> deferred = reader.readSnapshot();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            assertTrue(deferred.isEmpty(), "snapshot must be deferred while the writer is busy");
            assertTrue(elapsedMs < 5_000, "busy pool must be detected without waiting: " + elapsedMs + "ms");
        }

        assertTrue(reader.readSnapshot().isPresent());
    }

    private void writeTable(String table, String query) throws SQLException {
        try (Connection conn = writerDataSource.getConnection()) {
            helper.prepareConnectionForDuckLake(conn, false, false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE ducklake_catalog.main.\"" + table + "\" AS " + query);
            }
        }
    }

    private void appendRows(String table, String query) throws SQLException {
        try (Connection conn = writerDataSource.getConnection()) {
            helper.prepareConnectionForDuckLake(conn, false, false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO ducklake_catalog.main.\"" + table + "\" " + query);
            }
        }
    }

    static boolean duckLakeAvailable() {
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSTALL ducklake");
            stmt.execute("LOAD ducklake");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static Environment environment() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.datasource.url"))
                .thenReturn("jdbc:postgresql://localhost:5432/yaci?currentSchema=cardano");
        when(environment.getProperty("spring.datasource.username")).thenReturn("user");
        when(environment.getProperty("spring.datasource.password")).thenReturn("password");
        return environment;
    }
}
