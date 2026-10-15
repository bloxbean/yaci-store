package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckDb;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeCatalog;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeFile;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class SourceKeyCountTest {

    @TempDir
    Path dataDir;

    /**
     * address_utxo has one row per asset and is counted by distinct (tx_hash, output_index). The count
     * runs per partition; an output's assets may still be split across files of the same partition.
     */
    @Test
    void addressUtxoCountsDistinctOutputsPerPartition() throws Exception {
        try (Connection duck = DuckDb.open("256MB", dataDir.resolve("work"), 1)) {
            // day 1: output a#0 has two assets written to two files; a#1 has one
            List<DuckLakeFile> files = List.of(
                    write(duck, "main/address_utxo/date=2026-01-01/f1.parquet",
                            "('a', 0, 10), ('a', 1, 10)"),
                    write(duck, "main/address_utxo/date=2026-01-01/f2.parquet",
                            "('a', 0, 10)"),
                    // day 2: b#0 is at the cutoff, c#0 is beyond it
                    write(duck, "main/address_utxo/date=2026-01-02/f3.parquet",
                            "('b', 0, 20), ('b', 0, 20), ('c', 0, 21)"));

            var catalog = mock(DuckLakeCatalog.class);
            when(catalog.columns("address_utxo", 1)).thenReturn(Map.of(
                    "tx_hash", "VARCHAR", "output_index", "INTEGER", "slot", "BIGINT"));
            when(catalog.files("address_utxo", 1)).thenReturn(files);
            when(catalog.connection()).thenReturn(duck);
            when(catalog.parquetList(anyList())).thenAnswer(inv -> {
                List<DuckLakeFile> group = inv.getArgument(0);
                return group.stream()
                        .map(f -> "'" + dataDir.resolve(f.relativePath()) + "'")
                        .collect(Collectors.joining(", ", "[", "]"));
            });

            var spec = SnapshotSpecRegistry.builtIn().byId("address-utxo").orElseThrow();
            TablePlan plan = new ExportPlanner(catalog).plan(spec, 1, 300, 20);

            assertThat(plan.rowCount()).isEqualTo(3); // a#0, a#1, b#0
            verify(catalog, times(2)).parquetList(anyList()); // one query per partition
        }
    }

    private DuckLakeFile write(Connection duck, String relativePath, String rows) throws Exception {
        Path file = dataDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        DuckDb.exec(duck, "COPY (SELECT * FROM (VALUES " + rows + ") v(tx_hash, output_index, slot)) TO '"
                + file + "' (FORMAT parquet)");
        return new DuckLakeFile(relativePath, 1, Files.size(file));
    }
}
