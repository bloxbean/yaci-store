package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.ConsistencyPoint;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;

import java.util.List;
import java.util.Map;

/**
 * Read-only result of {@code snapshot inspect}: the point that would be chosen, why, what would be
 * packaged, and everything that would block publication.
 *
 * <p>{@code incompletePartitions} lists, per spec id, every partition an incomplete-export blocker
 * refers to; the blocker itself names only the first few.
 */
public record InspectionReport(long ducklakeSnapshotId,
                               String duckdbVersion,
                               Map<String, String> catalogMetadata,
                               int completedEpoch,
                               ConsistencyPoint point,
                               Map<String, Integer> gatingEpochs,
                               List<String> limitedBy,
                               long maxExportedBlock,
                               List<TablePlan> plans,
                               List<SnapshotTableSpec> nonImportedTables,
                               List<String> declaredLossy,
                               List<String> blockers,
                               List<String> warnings,
                               Map<String, List<String>> incompletePartitions) {

    /** Tables whose blocker names only some of their incomplete partitions. */
    public Map<String, List<String>> truncatedPartitionLists() {
        Map<String, List<String>> out = new java.util.LinkedHashMap<>();
        incompletePartitions.forEach((spec, list) -> {
            if (list.size() > ExportCoverage.SHOWN) {
                out.put(spec, list);
            }
        });
        return out;
    }

    public long estimatedArchiveBytes() {
        return plans.stream().mapToLong(TablePlan::totalBytes).sum();
    }

    public long fileCount() {
        return plans.stream().mapToLong(p -> p.files().size()).sum();
    }

    public long totalRows() {
        return plans.stream().mapToLong(TablePlan::rowCount).sum();
    }

    public boolean canExport() {
        return blockers.isEmpty();
    }
}
