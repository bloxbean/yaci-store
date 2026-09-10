package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.spec.CutoffType;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeFile;
import com.bloxbean.cardano.yaci.store.snapshot.spec.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Completion evidence includes zero-row partitions; row maxima cannot prove export completeness. */
public final class ExportCoverage {
    private ExportCoverage() {}

    public static List<String> check(SnapshotTableSpec spec, List<DuckLakeFile> files,
                                     Map<String, Long> completed, LocalDate firstDay,
                                     LocalDate lastDay, int firstNonByronEpoch, int completedEpoch, long cutSlot) {
        Map<String, Long> observed = new TreeMap<>();
        for (DuckLakeFile file : files) {
            String dir = file.partition();
            String partition = dir.substring(dir.lastIndexOf('/') + 1);
            observed.merge(partition, file.rowCount(), Long::sum);
        }
        List<String> required = new ArrayList<>();
        if (spec.source().partition().strategy() == PartitionStrategy.DAILY) {
            for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
                required.add("date=" + day);
            }
        } else if (spec.source().partition().strategy() == PartitionStrategy.EPOCH) {
            if (firstNonByronEpoch < 0) {
                return List.of("[" + spec.id() + "] cannot determine the first non-Byron epoch from exported blocks");
            }
            long lastEpoch = ExportPlanner.cutoffValue(spec.consistency().cutoff(), completedEpoch, cutSlot);
            // Epoch partitions with a slot cutoff (mir) are still required through the selected epoch.
            if (spec.consistency().cutoff().type() == CutoffType.SLOT_LTE
                    || lastEpoch == Long.MAX_VALUE) {
                lastEpoch = completedEpoch;
            }
            for (int epoch = firstNonByronEpoch; epoch <= lastEpoch; epoch++) {
                required.add("epoch=" + epoch);
            }
        } else {
            return List.of("[" + spec.id() + "] partition completion checks require DAILY or EPOCH partitioning");
        }
        List<String> problems = new ArrayList<>();
        for (String partition : required) {
            Long expected = completed.get(partition);
            long actual = observed.getOrDefault(partition, 0L);
            if (expected == null || expected < 0) {
                problems.add(partition + " has no completed export record");
            } else if (expected != actual) {
                problems.add(partition + " has " + actual + " catalog rows, export journal records " + expected);
            }
        }
        if (problems.isEmpty()) {
            return List.of();
        }
        return List.of("[" + spec.id() + "] incomplete export: "
                + String.join("; ", problems.subList(0, Math.min(5, problems.size())))
                + (problems.size() > 5 ? " (" + problems.size() + " partitions affected)" : "")
                + ". Backfill these partitions, including empty ones, before snapshot export.");
    }
}
