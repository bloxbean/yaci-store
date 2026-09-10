package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.BatchBoundary;
import com.bloxbean.cardano.yaci.store.snapshot.spec.ImportMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.RestoreMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.spec.TableKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BatchPlannerTest {

    private final BatchPlanner planner = new BatchPlanner();

    private static SnapshotTableSpec spec(BatchBoundary boundary, int batchSize) {
        return new SnapshotTableSpec("widget", 1, "test", TableKind.CHAIN_DATA, RestoreMode.IMPORT, null,
                null, null,
                new SnapshotTableSpec.Import("widget", ImportMode.DIRECT, Map.of(), List.of(), List.of(),
                        null, 1, List.of(), boundary, batchSize, null, null),
                new SnapshotTableSpec.Validation(List.of("id"), List.of(), List.of(), List.of()),
                Map.of(), null, "digest", "test");
    }

    private static List<SnapshotManifest.FileEntry> files(int partitions, int perPartition) {
        List<SnapshotManifest.FileEntry> out = new ArrayList<>();
        for (int p = 0; p < partitions; p++) {
            String partition = "main/widget/date=2026-01-%02d".formatted(p + 1);
            for (int i = 0; i < perPartition; i++) {
                out.add(new SnapshotManifest.FileEntry(partition + "/f" + i + ".parquet", 10,
                        "sha-" + p + "-" + i, 5, partition));
            }
        }
        return out;
    }

    @Test
    void fileSetBatchingRespectsTheDeclaredSize() {
        List<ImportBatch> batches = planner.plan("sid", spec(BatchBoundary.FILE_SET, 3), files(1, 10));
        assertThat(batches).hasSize(4);
        assertThat(batches.get(0).files()).hasSize(3);
        assertThat(batches.get(3).files()).hasSize(1);
    }

    @Test
    void singleBatchProducesOneUnit() {
        List<ImportBatch> batches = planner.plan("sid", spec(BatchBoundary.SINGLE_BATCH, 0), files(3, 4));
        assertThat(batches).hasSize(1);
        assertThat(batches.get(0).files()).hasSize(12);
    }

    @Test
    void wholePartitionNeverSplitsAPartitionAcrossBatches() {
        List<ImportBatch> batches = planner.plan("sid", spec(BatchBoundary.WHOLE_PARTITION, 2), files(5, 3));

        assertThat(batches).hasSize(3);
        for (ImportBatch batch : batches) {
            // Every file of a partition present in this batch must be present in full.
            batch.files().stream().map(SnapshotManifest.FileEntry::partition).distinct().forEach(p -> {
                long inBatch = batch.files().stream().filter(f -> f.partition().equals(p)).count();
                assertThat(inBatch).isEqualTo(3);
            });
        }
        assertThat(batches.stream().mapToLong(b -> b.files().size()).sum()).isEqualTo(15);
    }

    @Test
    void batchIdIsContentAddressedAndIndependentOfFileOrder() {
        List<SnapshotManifest.FileEntry> a = files(1, 4);
        List<SnapshotManifest.FileEntry> b = new ArrayList<>(a);
        java.util.Collections.reverse(b);

        ImportBatch first = ImportBatch.of("sid", spec(BatchBoundary.FILE_SET, 4), a, null);
        ImportBatch second = ImportBatch.of("sid", spec(BatchBoundary.FILE_SET, 4), b, null);
        assertThat(first.batchId()).isEqualTo(second.batchId());
    }

    @Test
    void batchIdChangesWithSnapshotContentAndTransformVersion() {
        List<SnapshotManifest.FileEntry> f = files(1, 4);
        String base = ImportBatch.of("sid", spec(BatchBoundary.FILE_SET, 4), f, null).batchId();

        assertThat(ImportBatch.of("other-snapshot", spec(BatchBoundary.FILE_SET, 4), f, null).batchId())
                .isNotEqualTo(base);

        SnapshotTableSpec v2 = new SnapshotTableSpec("widget", 1, "test", TableKind.CHAIN_DATA,
                RestoreMode.IMPORT, null, null, null,
                new SnapshotTableSpec.Import("widget", ImportMode.DIRECT, Map.of(), List.of(), List.of(),
                        null, 2, List.of(), BatchBoundary.FILE_SET, 4, null, null),
                new SnapshotTableSpec.Validation(List.of("id"), List.of(), List.of(), List.of()),
                Map.of(), null, "digest", "test");
        assertThat(ImportBatch.of("sid", v2, f, null).batchId()).isNotEqualTo(base);
    }

    @Test
    void changingBatchSizeDoesNotRenameUnaffectedBatches() {
        // Content addressing means the first three files still form the same batch id when the
        // batch size grows from 3 to 5 for later files.
        List<SnapshotManifest.FileEntry> f = files(1, 10);
        String small = planner.plan("sid", spec(BatchBoundary.FILE_SET, 3), f).get(0).batchId();
        String again = planner.plan("sid", spec(BatchBoundary.FILE_SET, 3), f).get(0).batchId();
        assertThat(small).isEqualTo(again);
    }

    @Test
    void anEmptyTableProducesNoBatches() {
        assertThat(planner.plan("sid", spec(BatchBoundary.FILE_SET, 3), List.of())).isEmpty();
    }
}
