package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.BatchBoundary;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Splits a table's manifest file list into batches according to its declared batch boundary. */
public class BatchPlanner {

    public static final int DEFAULT_FILE_BATCH = 64;

    public List<ImportBatch> plan(String snapshotId, SnapshotTableSpec spec,
                                  List<SnapshotManifest.FileEntry> files) {
        if (files.isEmpty()) {
            return List.of();
        }
        return switch (spec.importSpec().batchBoundary()) {
            case SINGLE_BATCH -> List.of(ImportBatch.of(snapshotId, spec, sorted(files), null));
            case FILE_SET -> fileSets(snapshotId, spec, files);
            case WHOLE_PARTITION -> wholePartitions(snapshotId, spec, files);
        };
    }

    private List<ImportBatch> fileSets(String snapshotId, SnapshotTableSpec spec,
                                       List<SnapshotManifest.FileEntry> files) {
        int size = spec.importSpec().batchSize() > 0 ? spec.importSpec().batchSize() : DEFAULT_FILE_BATCH;
        List<SnapshotManifest.FileEntry> ordered = sorted(files);
        List<ImportBatch> out = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i += size) {
            out.add(ImportBatch.of(snapshotId, spec,
                    ordered.subList(i, Math.min(i + size, ordered.size())), null));
        }
        return out;
    }

    /**
     * Whole partition directories. A transform that groups rows (address_utxo) must never see a
     * partition split across batches, or a grouped key could be emitted by two batches and violate
     * the primary key.
     */
    private List<ImportBatch> wholePartitions(String snapshotId, SnapshotTableSpec spec,
                                              List<SnapshotManifest.FileEntry> files) {
        Map<String, List<SnapshotManifest.FileEntry>> byPartition = new LinkedHashMap<>();
        for (SnapshotManifest.FileEntry f : sorted(files)) {
            byPartition.computeIfAbsent(f.partition(), k -> new ArrayList<>()).add(f);
        }
        int partitionsPerBatch = spec.importSpec().batchSize() > 0 ? spec.importSpec().batchSize() : 1;

        List<ImportBatch> out = new ArrayList<>();
        List<SnapshotManifest.FileEntry> current = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, List<SnapshotManifest.FileEntry>> e : byPartition.entrySet()) {
            current.addAll(e.getValue());
            names.add(e.getKey());
            if (names.size() >= partitionsPerBatch) {
                out.add(ImportBatch.of(snapshotId, spec, List.copyOf(current), String.join(",", names)));
                current.clear();
                names.clear();
            }
        }
        if (!current.isEmpty()) {
            out.add(ImportBatch.of(snapshotId, spec, List.copyOf(current), String.join(",", names)));
        }
        return out;
    }

    /** Path order makes batch composition, and therefore batch ids, reproducible. */
    private static List<SnapshotManifest.FileEntry> sorted(List<SnapshotManifest.FileEntry> files) {
        List<SnapshotManifest.FileEntry> copy = new ArrayList<>(files);
        copy.sort((a, b) -> a.path().compareTo(b.path()));
        return copy;
    }
}
