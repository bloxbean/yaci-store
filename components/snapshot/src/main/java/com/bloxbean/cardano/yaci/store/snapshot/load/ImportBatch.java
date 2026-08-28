package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;

import java.util.ArrayList;
import java.util.List;

/**
 * One independently committable unit of work.
 *
 * <p>The identity is content-addressed: it is derived from the snapshot id, the table, the transform
 * version and the sorted digests of the batch's input files. Retuning the batch size therefore does
 * not invalidate previously completed work the way an ordinal would, and an interrupted import
 * resumes by skipping exactly the batches already recorded.
 */
public record ImportBatch(String batchId,
                          SnapshotTableSpec spec,
                          List<SnapshotManifest.FileEntry> files,
                          String partition) {

    public static ImportBatch of(String snapshotId, SnapshotTableSpec spec,
                                 List<SnapshotManifest.FileEntry> files, String partition) {
        List<String> digests = new ArrayList<>(files.stream().map(SnapshotManifest.FileEntry::sha256).toList());
        digests.sort(String::compareTo);
        StringBuilder sb = new StringBuilder()
                .append(snapshotId).append('\n')
                .append(spec.id()).append('\n')
                .append(spec.specVersion()).append('\n')
                .append(spec.importSpec().transformVersion()).append('\n');
        digests.forEach(d -> sb.append(d).append('\n'));
        return new ImportBatch(Digests.sha256Hex(sb.toString()), spec, List.copyOf(files), partition);
    }

    public long rowsIn() {
        return files.stream().mapToLong(SnapshotManifest.FileEntry::rowCount).sum();
    }

    public long bytes() {
        return files.stream().mapToLong(SnapshotManifest.FileEntry::size).sum();
    }
}
