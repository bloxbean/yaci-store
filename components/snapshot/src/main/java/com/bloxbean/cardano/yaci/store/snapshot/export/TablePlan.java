package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeFile;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;

import java.util.List;
import java.util.Map;

/**
 * Everything the exporter resolved for one table at the pinned catalog snapshot: the exact immutable
 * file set to package, the row count that will survive the cutoff, and the observed bounds.
 *
 * @param rowCount rows at or before the cutoff — the number the importer must reproduce
 * @param exactRowCount false when the count had to be estimated because a straddling file could not
 *                      be measured; export refuses to publish in that case
 */
public record TablePlan(SnapshotTableSpec spec,
                        List<DuckLakeFile> files,
                        long rowCount,
                        boolean exactRowCount,
                        Map<String, String> sourceColumns,
                        Map<String, String> bounds,
                        String columnFingerprint,
                        List<String> problems) {

    public long totalBytes() {
        return files.stream().mapToLong(DuckLakeFile::sizeBytes).sum();
    }

    public boolean ok() {
        return problems.isEmpty();
    }
}
