package com.bloxbean.cardano.yaci.store.analytics.writer;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.ducklake.DuckLakeWriterLock;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exports into a real DuckLake catalog (DuckDB catalog file, in-memory DuckDB writer pool of one,
 * like the shipped writer). The queries are plain DuckDB queries, so no source database is attached.
 * Skipped when the DuckLake extension cannot be loaded (no network / not cached).
 */
class DuckLakeWriterServiceReplaceTest {

    // 2024-03-01T00:00:00Z
    private static final long MARCH_1 = 1709251200L;

    @TempDir
    Path tempDir;

    private HikariDataSource dataSource;
    private DuckLakeWriterService writer;
    private String exportPath;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(duckLakeAvailable(), "DuckLake extension not available");

        exportPath = tempDir.resolve("analytics").toString();
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(exportPath);
        properties.getStorage().setType("ducklake");
        properties.getDucklake().setCatalogType("duckdb");
        properties.getDucklake().setCatalogPath(tempDir.resolve("analytics/ducklake.catalog.db").toString());

        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:duckdb:");
        dataSource.setDriverClassName("org.duckdb.DuckDBDriver");
        dataSource.setMaximumPoolSize(1);
        dataSource.setMinimumIdle(1);

        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(environment(), properties) {
            @Override
            public void prepareConnectionForDuckLake(Connection conn, boolean needsSource, boolean readOnly)
                    throws SQLException {
                super.prepareConnectionForDuckLake(conn, false, readOnly);
            }
        };
        try (Connection conn = dataSource.getConnection()) {
            helper.prepareConnectionForDuckLake(conn, false, false);
            helper.configureDuckLakeCatalogSettings(conn);
        }
        writer = new DuckLakeWriterService(dataSource, properties, helper, new DuckLakeWriterLock());
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void reExportReplacesTheDayInsteadOfAppending() throws Exception {
        String march1 = blocks(0, 5, MARCH_1);
        String march2 = blocks(100, 3, MARCH_1 + 86400);
        assertThat(writer.export(march1, path("block", "date=2024-03-01"), "block_time").getRowCount()).isEqualTo(5);
        writer.export(march2, path("block", "date=2024-03-02"), "block_time");

        // Same day again, now with one more row: the day is replaced, the other day is untouched
        assertThat(writer.export(blocks(0, 6, MARCH_1), path("block", "date=2024-03-01"), "block_time")
                .getRowCount()).isEqualTo(6);
        writer.export(march2, path("block", "date=2024-03-02"), "block_time");

        assertThat(rowsPerDate("block")).containsExactly(Map.entry("2024-03-01", 6L), Map.entry("2024-03-02", 3L));
        // Whole partitions were deleted: old data files retired, no deletion files (snapshot export refuses those)
        assertThat(liveDeleteFiles()).isZero();
        assertThat(liveDataFiles("date=2024-03-01")).isEqualTo(1);
        assertThat(writer.verifyExport(path("block", "date=2024-03-01"), 6)).isTrue();
    }

    @Test
    void rowsAreFiledUnderTheExportedDayNotTheirBlockTime() throws Exception {
        // The day's slot range selected these rows: one with a block time before midnight, one
        // without a block time (exporters write to_timestamp(COALESCE(block_time, 0))).
        String query = "SELECT * FROM (VALUES (10::BIGINT, to_timestamp(" + (MARCH_1 - 9) + ")), "
                + "(11::BIGINT, to_timestamp(0))) AS t(slot, block_time)";

        writer.export(query, path("stake_address_balance", "date=2024-03-01"), "block_time");

        assertThat(rowsPerDate("stake_address_balance")).containsExactly(Map.entry("2024-03-01", 2L));
    }

    @Test
    void reExportReplacesTheEpoch() throws Exception {
        String query = "SELECT 450 AS epoch, range AS amount FROM range(3)";
        writer.export(query, path("reward", "epoch=450"), "block_time");
        writer.export("SELECT 451 AS epoch, range AS amount FROM range(2)", path("reward", "epoch=451"), "block_time");
        writer.export(query, path("reward", "epoch=450"), "block_time");

        assertThat(count("SELECT COUNT(*) FROM reward WHERE epoch = 450")).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM reward WHERE epoch = 451")).isEqualTo(2);
        assertThat(liveDeleteFiles()).isZero();
    }

    @Test
    void failedReExportKeepsThePreviousRows() throws Exception {
        writer.export(blocks(0, 5, MARCH_1), path("block", "date=2024-03-01"), "block_time");

        // Fails inside the transaction, after the DELETE
        String failing = "SELECT range AS slot, to_timestamp(" + MARCH_1 + " + range) AS block_time "
                + "FROM range(5) WHERE error('source failed') IS NULL";
        assertThatThrownBy(() -> writer.export(failing, path("block", "date=2024-03-01"), "block_time"))
                .isInstanceOf(RuntimeException.class);

        assertThat(rowsPerDate("block")).containsExactly(Map.entry("2024-03-01", 5L));
    }

    @Test
    void purgesTheEpochZeroPartitionOnlyWhenTheRepairedDaysCoverAllOfIt() throws Exception {
        writer.export(blocks(0, 5, MARCH_1), path("stake_address_balance", "date=2024-03-01"), "block_time");
        // A row exported before the partition date was authoritative, and since re-exported under its day
        execute("INSERT INTO stake_address_balance VALUES (3, to_timestamp(0), DATE '1970-01-01')");

        DuckLakeWriterService.EpochZeroPartition outside = writer.purgeEpochZeroPartition("stake_address_balance", 0, 3);
        assertThat(outside).isEqualTo(new DuckLakeWriterService.EpochZeroPartition(1, 3L, 3L, false));
        assertThat(rowsPerDate("stake_address_balance")).containsEntry("1970-01-01", 1L);

        DuckLakeWriterService.EpochZeroPartition covered = writer.purgeEpochZeroPartition("stake_address_balance", 0, 5);
        assertThat(covered).isEqualTo(new DuckLakeWriterService.EpochZeroPartition(1, 3L, 3L, true));
        assertThat(rowsPerDate("stake_address_balance")).containsExactly(Map.entry("2024-03-01", 5L));
        assertThat(liveDeleteFiles()).isZero();

        assertThat(writer.purgeEpochZeroPartition("stake_address_balance", 0, 5))
                .isEqualTo(new DuckLakeWriterService.EpochZeroPartition(0, null, null, false));
        // Tables without a slot column, or not exported at all, are left alone
        assertThat(writer.purgeEpochZeroPartition("no_such_table", 0, 5).deleted()).isFalse();
    }

    private static String blocks(long firstSlot, int count, long firstTime) {
        return "SELECT " + firstSlot + " + range AS slot, to_timestamp(" + firstTime + " + range) AS block_time "
                + "FROM range(" + count + ")";
    }

    private String path(String table, String partition) {
        return Path.of(exportPath, table, partition, "data.parquet").toString();
    }

    private Map<String, Long> rowsPerDate(String table) throws SQLException {
        Map<String, Long> rows = new LinkedHashMap<>();
        try (Connection conn = writerConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT CAST(date AS VARCHAR), COUNT(*) FROM " + table
                     + " GROUP BY ALL ORDER BY 1")) {
            while (rs.next()) {
                rows.put(rs.getString(1), rs.getLong(2));
            }
        }
        return rows;
    }

    private long liveDeleteFiles() throws SQLException {
        return count("SELECT COUNT(*) FROM __ducklake_metadata_ducklake_catalog.ducklake_delete_file "
                + "WHERE end_snapshot IS NULL");
    }

    private long liveDataFiles(String partition) throws SQLException {
        return count("SELECT COUNT(*) FROM __ducklake_metadata_ducklake_catalog.ducklake_data_file "
                + "WHERE end_snapshot IS NULL AND path LIKE '" + partition + "/%'");
    }

    private long count(String sql) throws SQLException {
        try (Connection conn = writerConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection conn = writerConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /** The pool's single connection, which the writer left attached and on ducklake_catalog. */
    private Connection writerConnection() throws SQLException {
        Connection conn = dataSource.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("USE ducklake_catalog");
        }
        return conn;
    }

    private static boolean duckLakeAvailable() {
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
