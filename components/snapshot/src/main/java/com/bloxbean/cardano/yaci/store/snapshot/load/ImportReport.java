package com.bloxbean.cardano.yaci.store.snapshot.load;

import java.util.List;
import java.util.Map;

/**
 * Outcome of an import run.
 *
 * <p>Durations are reported per phase so archive verification, heap load, freezing, the rebuild of the keys and
 * indexes deferred during the load, planner statistics, and control-state work can be read separately. The later manual
 * {@code apply-indexes} step for optional indexes is deliberately not part of this: it follows the
 * normal admin CLI workflow and has its own timings.
 */
public record ImportReport(String snapshotId,
                           String status,
                           long batchesPlanned,
                           long batchesSkipped,
                           long batchesLoaded,
                           long rowsLoaded,
                           Map<String, Long> rowsPerTable,
                           List<String> handlerResults,
                           Map<String, Long> sequencesReset,
                           int partitionsCreated,
                           List<String> declaredLimitations,
                           List<String> warnings,
                           long verifyMillis,
                           long extractMillis,
                           long loadMillis,
                           long freezeMillis,
                           long indexMillis,
                           long analyzeMillis,
                           long controlStateMillis) {

    public long totalMillis() {
        return verifyMillis + extractMillis + loadMillis + freezeMillis + indexMillis + analyzeMillis + controlStateMillis;
    }
}
