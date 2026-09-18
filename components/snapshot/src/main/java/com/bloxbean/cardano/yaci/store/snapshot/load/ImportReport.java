package com.bloxbean.cardano.yaci.store.snapshot.load;

import java.util.List;
import java.util.Map;

/**
 * Outcome of an import run.
 *
 * <p>Durations are reported per phase so archive verification, heap load and control-state work can
 * be read separately. The later manual {@code apply-indexes} step is deliberately not part of this:
 * index creation follows the normal admin CLI workflow and has its own timings.
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
                           long controlStateMillis) {

    public long totalMillis() {
        return verifyMillis + extractMillis + loadMillis + controlStateMillis;
    }
}
