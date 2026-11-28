package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import lombok.Getter;

/**
 * A requested export or repair covers a partition that is not ready (see
 * {@link ExportReadinessService}). Nothing was exported and no state was changed.
 */
@Getter
public class PartitionNotReadyException extends RuntimeException {

    private final PartitionReadiness readiness;

    public PartitionNotReadyException(PartitionReadiness readiness) {
        super(readiness.reason());
        this.readiness = readiness;
    }
}
