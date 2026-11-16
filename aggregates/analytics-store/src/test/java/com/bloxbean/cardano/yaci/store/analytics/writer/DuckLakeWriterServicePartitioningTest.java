package com.bloxbean.cardano.yaci.store.analytics.writer;

import com.bloxbean.cardano.yaci.store.analytics.ducklake.DuckLakeTestCatalog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Table creation, partitioning and verification against a real DuckLake catalog.
 */
class DuckLakeWriterServicePartitioningTest {

    // 2024-03-01T00:00:00Z
    private static final long MARCH_1 = 1709251200L;

    @TempDir
    Path tempDir;

    private DuckLakeTestCatalog catalog;
    private DuckLakeWriterService writer;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(DuckLakeTestCatalog.duckLakeAvailable(), "DuckLake extension not available");
        catalog = new DuckLakeTestCatalog(tempDir);
        writer = catalog.writer();
    }

    @AfterEach
    void tearDown() {
        if (catalog != null) {
            catalog.close();
        }
    }

    @Test
    void anEpochQueryWithoutAnEpochColumnFailsBeforeCreatingTheTable() throws Exception {
        // A custom EPOCH exporter whose query does not select the epoch
        String query = "SELECT range AS amount FROM range(3)";

        assertThatThrownBy(() -> writer.export(query, catalog.path("pool_rewards", "epoch=450"), "block_time"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("returns no 'epoch' column");

        // Before the fix the table was created (and committed) without partitioning, and every
        // later export failed
        assertThat(tableCount("pool_rewards")).isZero();

        // Once the query selects the epoch, the table is created partitioned and the export verifies
        String fixed = "SELECT 450 AS epoch, range AS amount FROM range(3)";
        assertThat(writer.export(fixed, catalog.path("pool_rewards", "epoch=450"), "block_time").getRowCount())
                .isEqualTo(3);
        assertThat(partitionKeys("pool_rewards")).containsExactly("epoch");
        assertThat(writer.verifyExport(catalog.path("pool_rewards", "epoch=450"), 3)).isTrue();
    }

    @Test
    void aNewTableIsCreatedPartitioned() throws Exception {
        writer.export(blocks(0, 5, MARCH_1), catalog.path("block", "date=2024-03-01"), "block_time");

        assertThat(partitionKeys("block")).containsExactly("date");
        assertThat(parquetFiles("main/block/date=2024-03-01")).hasSize(1);
    }

    @Test
    void anUnpartitionedTableIsPartitionedAtTheNextExport() throws Exception {
        // What an interrupted create left behind: the table committed, its partitioning never set,
        // and exports since then written unpartitioned, one day per file
        catalog.execute("CREATE TABLE block (slot BIGINT, block_time TIMESTAMP WITH TIME ZONE, date DATE)");
        catalog.execute("INSERT INTO block SELECT range, to_timestamp(" + MARCH_1 + " + range), DATE '2024-03-01' "
                + "FROM range(5)");
        catalog.execute("INSERT INTO block SELECT 100 + range, to_timestamp(" + (MARCH_1 + 86400) + " + range), "
                + "DATE '2024-03-02' FROM range(3)");
        assertThat(partitionKeys("block")).isEmpty();

        writer.export(blocks(0, 6, MARCH_1), catalog.path("block", "date=2024-03-01"), "block_time");

        assertThat(partitionKeys("block")).containsExactly("date");
        assertThat(catalog.count("SELECT COUNT(*) FROM block WHERE date = DATE '2024-03-01'")).isEqualTo(6);
        assertThat(catalog.count("SELECT COUNT(*) FROM block WHERE date = DATE '2024-03-02'")).isEqualTo(3);
        // The old file of the day was dropped whole: no delete file the query layer could not apply
        assertThat(catalog.count("SELECT COUNT(*) FROM ducklake_list_files('ducklake_catalog', 'block') "
                + "WHERE delete_file IS NOT NULL")).isZero();
        // The new file is in its day's folder, and the export verifies
        assertThat(parquetFiles("main/block/date=2024-03-01")).hasSize(1);
        assertThat(writer.verifyExport(catalog.path("block", "date=2024-03-01"), 6)).isTrue();
    }

    @Test
    void anUnpartitionedTableWithMixedFilesIsRefused() throws Exception {
        catalog.execute("CREATE TABLE block (slot BIGINT, block_time TIMESTAMP WITH TIME ZONE, date DATE)");
        catalog.execute("INSERT INTO block VALUES (1, to_timestamp(" + MARCH_1 + "), DATE '2024-03-01'), "
                + "(2, to_timestamp(" + (MARCH_1 + 86400) + "), DATE '2024-03-02')");

        assertThatThrownBy(() -> writer.export(blocks(0, 6, MARCH_1), catalog.path("block", "date=2024-03-01"),
                "block_time"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Rebuild the table");

        assertThat(partitionKeys("block")).isEmpty();
        assertThat(catalog.count("SELECT COUNT(*) FROM block")).isEqualTo(2);
    }

    @Test
    void aTablePartitionedByAnotherKeyIsRefused() throws Exception {
        catalog.execute("CREATE TABLE reward (epoch INTEGER, amount BIGINT, date DATE)");
        catalog.execute("ALTER TABLE reward SET PARTITIONED BY (date)");

        assertThatThrownBy(() -> writer.export("SELECT 450 AS epoch, range AS amount FROM range(3)",
                catalog.path("reward", "epoch=450"), "block_time"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("partitioned by [date]");
    }

    @Test
    void verificationCountsTheCatalogNotTheFilesOnDisk() throws Exception {
        String path = catalog.path("block", "date=2024-03-01");
        writer.export(blocks(0, 5, MARCH_1), path, "block_time");
        assertThat(writer.verifyExport(path, 5)).isTrue();
        assertThat(writer.verifyExport(path, 4)).isFalse();

        // Replace the day with no rows: the retired file stays on disk, but the catalog has nothing
        writer.export("SELECT * FROM (" + blocks(0, 5, MARCH_1) + ") WHERE slot < 0", path, "block_time");
        assertThat(parquetFiles("main/block/date=2024-03-01")).isNotEmpty();
        assertThat(writer.verifyExport(path, 5)).isFalse();

        // A table the catalog does not know (recreated catalog) never verifies
        assertThat(writer.verifyExport(catalog.path("transaction", "date=2024-03-01"), 5)).isFalse();
    }

    private long tableCount(String table) throws Exception {
        return catalog.count("SELECT COUNT(*) FROM duckdb_tables() WHERE database_name = 'ducklake_catalog' "
                + "AND table_name = '" + table + "'");
    }

    private List<String> partitionKeys(String table) throws Exception {
        try (Connection conn = catalog.connection()) {
            return writer.partitionKeys(conn, table);
        }
    }

    private List<Path> parquetFiles(String dir) throws Exception {
        Path path = Path.of(catalog.exportPath, dir);
        if (!Files.isDirectory(path)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(path)) {
            return files.filter(f -> f.toString().endsWith(".parquet")).toList();
        }
    }

    private static String blocks(long firstSlot, int count, long firstTime) {
        return "SELECT " + firstSlot + " + range AS slot, to_timestamp(" + firstTime + " + range) AS block_time "
                + "FROM range(" + count + ")";
    }
}
