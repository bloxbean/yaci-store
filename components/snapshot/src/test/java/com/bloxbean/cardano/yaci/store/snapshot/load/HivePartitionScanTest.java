package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.convert.ConverterRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckDb;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DuckLake writes files into {@code epoch=N} and {@code date=D} directories. Left to its default,
 * {@code read_parquet} replaces the file's own column with the directory value, so these files
 * deliberately disagree with their directories: every snapshot scan must return the file's value
 * and type.
 */
class HivePartitionScanTest {

    @TempDir
    Path dir;

    @Test
    void aScanReadsTheFileColumnsNotTheDirectoryValues() throws Exception {
        try (Connection duck = DuckDb.open("256MB", dir.resolve("work"), 1)) {
            Path epochFile = write(duck, "main/adapot/epoch=208/f.parquet", "SELECT 207::INTEGER epoch");
            Path dateFile = write(duck, "main/block/date=2020-08-08/b.parquet", "SELECT DATE '2020-08-09' date");

            assertThat(describe(duck, "SELECT * FROM read_parquet(['" + epochFile + "'])"))
                    .as("the DuckDB default this guards against").containsEntry("epoch", "BIGINT");

            assertThat(describe(duck, "SELECT * FROM " + DuckDb.readParquet("['" + epochFile + "']")))
                    .containsEntry("epoch", "INTEGER");
            assertThat(single(duck, "SELECT epoch FROM " + DuckDb.readParquet("['" + epochFile + "']")))
                    .isEqualTo(207);
            assertThat(single(duck, "SELECT CAST(date AS VARCHAR) FROM "
                    + DuckDb.readParquet("['" + dateFile + "']"))).isEqualTo("2020-08-09");
        }
    }

    @Test
    void theLoaderReadsFileColumnsInEveryImportMode() throws Exception {
        try (Connection duck = DuckDb.open("256MB", dir.resolve("work"), 1)) {
            write(duck, "main/adapot/epoch=208/f.parquet", "SELECT 207::INTEGER epoch, 100::BIGINT slot");
            write(duck, "main/mir/epoch=208/m.parquet", """
                    SELECT 'aa'::VARCHAR tx_hash, 0::INTEGER cert_index, 'RESERVES'::VARCHAR pot,
                           'cred'::VARCHAR credential, 'stake1x'::VARCHAR address, 5::DECIMAL(38,0) amount,
                           207::INTEGER epoch, 100::BIGINT slot, 'bh'::VARCHAR block_hash,
                           TIMESTAMPTZ '2020-08-08 21:44:51+00' block_time""");
            write(duck, "main/block/date=2020-08-08/b.parquet", "SELECT 'bh'::VARCHAR hash, 7::BIGINT number");

            SnapshotSpecRegistry registry = SnapshotSpecRegistry.builtIn();
            TableLoader loader = new TableLoader(new ConverterRegistry(), dir);

            // MAPPED, with an EPOCH_LTE cutoff on the partition column: the directory value 208 would
            // also drop the row at completed epoch 207.
            var adapot = registry.byId("adapot").orElseThrow();
            String mapped = loader.sourceSelect(adapot,
                    ImportBatch.of("s", adapot, List.of(entry("main/adapot/epoch=208/f.parquet")), "epoch=208"),
                    100, 207, Map.of());
            assertThat(describe(duck, "SELECT * FROM " + mapped)).containsEntry("epoch", "INTEGER");
            assertThat(single(duck, "SELECT epoch FROM " + mapped)).isEqualTo(207);

            var mir = registry.byId("mir").orElseThrow();
            String sql = loader.sourceSelect(mir,
                    ImportBatch.of("s", mir, List.of(entry("main/mir/epoch=208/m.parquet")), "epoch=208"),
                    100, 207, Map.of("block", List.of(entry("main/block/date=2020-08-08/b.parquet"))));
            assertThat(single(duck, "SELECT epoch FROM " + sql)).isEqualTo(207);
        }
    }

    private static SnapshotManifest.FileEntry entry(String path) {
        return new SnapshotManifest.FileEntry(path, 1, "0".repeat(64), 1, path.substring(0, path.lastIndexOf('/')));
    }

    private Path write(Connection duck, String relativePath, String select) throws Exception {
        Path file = dir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        DuckDb.exec(duck, "COPY (" + select + ") TO '" + file + "' (FORMAT parquet)");
        return file;
    }

    private static Map<String, String> describe(Connection duck, String select) throws Exception {
        Map<String, String> out = new LinkedHashMap<>();
        try (ResultSet rs = duck.createStatement().executeQuery("DESCRIBE (" + select + ")")) {
            while (rs.next()) {
                out.put(rs.getString("column_name"), rs.getString("column_type"));
            }
        }
        return out;
    }

    private static Object single(Connection duck, String select) throws Exception {
        try (ResultSet rs = duck.createStatement().executeQuery(select)) {
            assertThat(rs.next()).isTrue();
            Object value = rs.getObject(1);
            assertThat(rs.next()).isFalse();
            return value;
        }
    }
}
