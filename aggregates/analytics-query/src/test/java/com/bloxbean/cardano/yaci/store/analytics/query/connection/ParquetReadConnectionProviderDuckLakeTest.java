package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.ducklake.DuckLakeCatalogSnapshotReader;
import com.bloxbean.cardano.yaci.store.analytics.ducklake.DuckLakeWriterLock;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end check of DuckLake storage mode with the shipped-style configuration (relative
 * export path, DuckDB-file catalog owned by the export writer):
 * <ul>
 *   <li>the query engine serves committed DuckLake data after {@code lockDown()} (relative
 *       catalog data paths must not break the {@code allowed_directories} sandbox),</li>
 *   <li>the writer keeps working while the query engine is up (no catalog file-handle
 *       conflict), and</li>
 *   <li>the periodic refresh picks up newly committed files.</li>
 * </ul>
 * Skipped when the DuckLake extension cannot be loaded.
 */
class ParquetReadConnectionProviderDuckLakeTest {

    private Path relativeExportPath;
    private HikariDataSource writerDataSource;
    private DuckDbConnectionHelper helper;
    private AnalyticsStoreProperties properties;
    private DuckLakeWriterLock writerLock;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(duckLakeAvailable(), "DuckLake extension not available");

        relativeExportPath = Path.of("build", "tmp", "ducklake-query-" + UUID.randomUUID(), "data", "analytics");
        Files.createDirectories(relativeExportPath);

        properties = new AnalyticsStoreProperties();
        properties.setExportPath("./" + relativeExportPath);
        properties.getStorage().setType("ducklake");
        properties.getDucklake().setCatalogType("duckdb");
        properties.getDucklake().setCatalogPath("./" + relativeExportPath.resolve("ducklake.catalog.db"));
        properties.getDuckdb().setThreads(2);
        properties.getDuckdb().getReader().setMaximumPoolSize(2);

        writerDataSource = new HikariDataSource();
        writerDataSource.setJdbcUrl("jdbc:duckdb:");
        writerDataSource.setDriverClassName("org.duckdb.DuckDBDriver");
        writerDataSource.setMaximumPoolSize(1);
        writerDataSource.setMinimumIdle(1);
        writerDataSource.setConnectionTimeout(1000);

        helper = new DuckDbConnectionHelper(environment(), properties);
        writerLock = new DuckLakeWriterLock();
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
    void servesCommittedDuckLakeDataUnderLockdownWhileWriterKeepsExporting() throws Exception {
        // Writer (holds the catalog read-write for the JVM lifetime, like DuckLakeWriterService)
        writeTable("block", "SELECT range AS slot, CAST('2024-01-01' AS DATE) AS date FROM range(10)");
        writeTable("adapot", "SELECT 450::INTEGER AS epoch, 100::BIGINT AS slot");
        writeTable("empty_table", "SELECT 1::BIGINT AS slot, 'x'::VARCHAR AS hash LIMIT 0");

        ParquetTableRegistry registry = new ParquetTableRegistry(properties);
        registry.init();
        DuckLakeCatalogSnapshotReader snapshotReader =
                new DuckLakeCatalogSnapshotReader(writerDataSource, helper, properties, writerLock);

        ParquetReadConnectionProvider provider = new ParquetReadConnectionProvider(
                properties, registry,
                objectProvider(DuckDbConnectionHelper.class, helper),
                objectProvider(CutoffSlotResolver.class, null),
                objectProvider(TableExporterRegistry.class, null),
                objectProvider(DuckLakeCatalogSnapshotReader.class, snapshotReader));
        provider.createViews(); // discovers, creates views, locks the instance down

        assertEquals(List.of("adapot", "block", "empty_table"), registry.getTableNames());
        assertEquals(10, count(provider, "SELECT count(*) FROM block"));
        assertEquals(1, count(provider, "SELECT count(*) FROM adapot WHERE epoch = 450"));
        assertEquals(0, count(provider, "SELECT count(*) FROM empty_table"));
        assertEquals(2, count(provider, "SELECT count(*) FROM (DESCRIBE empty_table)"));

        // Sandbox is in force on the shared instance
        try (Connection conn = provider.getReadConnection(); Statement stmt = conn.createStatement()) {
            assertThrows(SQLException.class, () -> stmt.execute("SET enable_external_access = true"));
            assertThrows(SQLException.class, () -> stmt.execute("ATTACH ':memory:' AS other"));
        }

        // The writer must still be able to export (no 'Unique file handle conflict')
        appendRows("block", "SELECT range + 100 AS slot, CAST('2024-01-02' AS DATE) AS date FROM range(5)");
        writeTable("committee", "SELECT 1::BIGINT AS slot, 'abc' AS hash");

        // Old snapshot until refresh; refresh picks up the new file and the new table
        assertEquals(10, count(provider, "SELECT count(*) FROM block"));
        provider.refreshViews();
        assertEquals(15, count(provider, "SELECT count(*) FROM block"));
        assertEquals(2, count(provider, "SELECT count(DISTINCT date) FROM block"));
        assertEquals(1, count(provider, "SELECT count(*) FROM committee"));
        assertEquals(List.of("adapot", "block", "committee", "empty_table"), registry.getTableNames());
    }

    @Test
    void servesTablesThatMixPartitionedAndUnpartitionedDataFiles() throws Exception {
        // First file written before partitioning was configured (no date= directory), later
        // files partitioned — DuckDB's hive_partitioning=true would reject this mix.
        writeTable("block", "SELECT range AS slot, CAST('2024-01-01' AS DATE) AS date FROM range(10)");
        try (Connection conn = writerDataSource.getConnection()) {
            helper.prepareConnectionForDuckLake(conn, false, false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ducklake_catalog.main.block SET PARTITIONED BY (date)");
            }
        }
        appendRows("block", "SELECT range + 100 AS slot, CAST('2024-01-02' AS DATE) AS date FROM range(5)");

        ParquetTableRegistry registry = new ParquetTableRegistry(properties);
        registry.init();
        DuckLakeCatalogSnapshotReader snapshotReader =
                new DuckLakeCatalogSnapshotReader(writerDataSource, helper, properties, writerLock);
        ParquetReadConnectionProvider provider = new ParquetReadConnectionProvider(
                properties, registry,
                objectProvider(DuckDbConnectionHelper.class, helper),
                objectProvider(CutoffSlotResolver.class, null),
                objectProvider(TableExporterRegistry.class, null),
                objectProvider(DuckLakeCatalogSnapshotReader.class, snapshotReader));
        provider.createViews();

        assertEquals(2, registry.getDuckLakeFiles("block").size());
        assertTrue(!ParquetReadConnectionProvider.hasUniformHiveLayout(registry.getDuckLakeFiles("block")));
        assertEquals(15, count(provider, "SELECT count(*) FROM block"));
        assertEquals(5, count(provider, "SELECT count(*) FROM block WHERE date = DATE '2024-01-02'"));
        // No DuckLake bookkeeping columns leak into the view
        assertEquals(0, count(provider,
                "SELECT count(*) FROM (DESCRIBE block) WHERE column_name LIKE '\\_ducklake\\_internal%' ESCAPE '\\'"));
    }

    @Test
    void epochPartitionedTablesKeepFileColumnTypeAndSupportAggregates() throws Exception {
        // Exporter pattern: empty CTAS, then partitioning, then INSERTs -> files under epoch=N/.
        writeTable("epoch_tbl", "SELECT 1::INTEGER AS epoch, 1::BIGINT AS slot LIMIT 0");
        try (Connection conn = writerDataSource.getConnection()) {
            helper.prepareConnectionForDuckLake(conn, false, false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE ducklake_catalog.main.epoch_tbl SET PARTITIONED BY (epoch)");
            }
        }
        appendRows("epoch_tbl", "SELECT 450::INTEGER AS epoch, 100::BIGINT AS slot UNION ALL SELECT 451, 200");

        ParquetTableRegistry registry = new ParquetTableRegistry(properties);
        registry.init();
        ParquetReadConnectionProvider provider = new ParquetReadConnectionProvider(
                properties, registry,
                objectProvider(DuckDbConnectionHelper.class, helper),
                objectProvider(CutoffSlotResolver.class, null),
                objectProvider(TableExporterRegistry.class, null),
                objectProvider(DuckLakeCatalogSnapshotReader.class,
                        new DuckLakeCatalogSnapshotReader(writerDataSource, helper, properties, writerLock)));
        provider.createViews();

        assertTrue(ParquetReadConnectionProvider.hasUniformHiveLayout(registry.getDuckLakeFiles("epoch_tbl")));
        // DuckDB 1.5.x fails MIN/MAX on a hive key whose auto type (BIGINT) differs from the
        // file column (INTEGER) unless hive_types pins it; the view must keep the file type.
        assertEquals(450, count(provider, "SELECT MIN(epoch) FROM epoch_tbl"));
        assertEquals(451, count(provider, "SELECT MAX(epoch) FROM epoch_tbl"));
        assertEquals(1, count(provider, "SELECT count(*) FROM epoch_tbl WHERE epoch = 451"));
        try (Connection conn = provider.getReadConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT typeof(epoch) FROM epoch_tbl LIMIT 1")) {
            rs.next();
            assertEquals("INTEGER", rs.getString(1));
        }
    }

    @Test
    void refreshKeepsCurrentViewsWhileAnExportHoldsTheWriter() throws Exception {
        writeTable("block", "SELECT range AS slot FROM range(4)");
        ParquetTableRegistry registry = new ParquetTableRegistry(properties);
        registry.init();
        DuckLakeCatalogSnapshotReader snapshotReader =
                new DuckLakeCatalogSnapshotReader(writerDataSource, helper, properties, writerLock);
        ParquetReadConnectionProvider provider = new ParquetReadConnectionProvider(
                properties, registry,
                objectProvider(DuckDbConnectionHelper.class, helper),
                objectProvider(CutoffSlotResolver.class, null),
                objectProvider(TableExporterRegistry.class, null),
                objectProvider(DuckLakeCatalogSnapshotReader.class, snapshotReader));
        provider.createViews();
        assertEquals(4, count(provider, "SELECT count(*) FROM block"));

        try (Connection heldByExport = writerDataSource.getConnection()) {
            provider.refreshViews(); // deferred, must not throw and must keep serving
            assertEquals(4, count(provider, "SELECT count(*) FROM block"));
        }
    }

    private static long count(ParquetReadConnectionProvider provider, String sql) throws SQLException {
        try (Connection conn = provider.getReadConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
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

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> objectProvider(Class<T> type, T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
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
