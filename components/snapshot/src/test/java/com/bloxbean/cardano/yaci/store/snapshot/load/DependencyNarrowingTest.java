package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.BatchBoundary;
import com.bloxbean.cardano.yaci.store.snapshot.spec.ImportMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.RestoreMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.spec.TableKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A SQL transform's dependency must be narrowed to the partitions the batch can actually reference,
 * or every batch re-reads the whole block export.
 */
class DependencyNarrowingTest {

    private static SnapshotManifest.FileEntry file(String partition, String name) {
        return new SnapshotManifest.FileEntry(partition + "/" + name, 10, "sha-" + partition + name, 5,
                partition);
    }

    private static ImportBatch batch(List<SnapshotManifest.FileEntry> files) {
        SnapshotTableSpec spec = new SnapshotTableSpec("address-utxo", 1, "utxo", TableKind.CHAIN_DATA,
                RestoreMode.IMPORT, null, null, null,
                new SnapshotTableSpec.Import("address_utxo", ImportMode.SQL, Map.of(), List.of(), List.of(),
                        "classpath:/snapshot/sql/address_utxo_v1.sql", 1, List.of("block"),
                        BatchBoundary.WHOLE_PARTITION, 1, null, null),
                new SnapshotTableSpec.Validation(List.of("tx_hash"), List.of(), List.of(), List.of()),
                Map.of(), "digest", "test");
        return ImportBatch.of("sid", spec, files, null);
    }

    @Test
    void keepsOnlyTheDependencyPartitionsTheBatchCanReference() {
        ImportBatch b = batch(List.of(
                file("main/address_utxo/date=2026-01-01", "a.parquet"),
                file("main/address_utxo/date=2026-01-02", "b.parquet")));
        List<SnapshotManifest.FileEntry> blocks = List.of(
                file("main/block/date=2026-01-01", "x.parquet"),
                file("main/block/date=2026-01-02", "y.parquet"),
                file("main/block/date=2026-01-03", "z.parquet"),
                file("main/block/date=2025-12-31", "w.parquet"));

        List<SnapshotManifest.FileEntry> narrowed = TableLoader.narrowToBatchPartitions(b, blocks);

        assertThat(narrowed).hasSize(2);
        assertThat(narrowed).extracting(SnapshotManifest.FileEntry::partition)
                .containsExactly("main/block/date=2026-01-01", "main/block/date=2026-01-02");
    }

    @Test
    void fallsBackToTheFullDependencyWhenPartitionKeysDoNotLineUp() {
        // mir is epoch-partitioned while block is date-partitioned: no key can match, so the join
        // must still see every block file rather than none.
        ImportBatch b = batch(List.of(file("main/mir/epoch=210", "a.parquet")));
        List<SnapshotManifest.FileEntry> blocks = List.of(
                file("main/block/date=2026-01-01", "x.parquet"),
                file("main/block/date=2026-01-02", "y.parquet"));

        assertThat(TableLoader.narrowToBatchPartitions(b, blocks)).isEqualTo(blocks);
    }

    @Test
    void fallsBackWhenAFileHasNoPartitionSegment() {
        ImportBatch b = batch(List.of(file("main/address_utxo", "a.parquet")));
        List<SnapshotManifest.FileEntry> blocks = List.of(file("main/block/date=2026-01-01", "x.parquet"));

        assertThat(TableLoader.narrowToBatchPartitions(b, blocks)).isEqualTo(blocks);
    }
}
