package com.bloxbean.cardano.yaci.store.analytics.scheduler;

/**
 * Whether a partition of a table can be exported now, and if not, why.
 *
 * @param status the readiness
 * @param reason a message for logs and admin responses; null when {@link Status#READY}
 */
public record PartitionReadiness(Status status, String reason) {

    public enum Status {
        /** The partition's data is final and can be exported. */
        READY,
        /** The partition is too recent: its data can still change (finality window) or has not been written. */
        NOT_FINAL,
        /** A job or module the table depends on has not finished the partition yet (for example the AdaPot job). */
        DEPENDENCY_PENDING,
        /** The exporter can never be ready with the current configuration (for example adapot disabled). */
        UNAVAILABLE
    }

    public static final PartitionReadiness READY = new PartitionReadiness(Status.READY, null);

    public static PartitionReadiness notFinal(String reason) {
        return new PartitionReadiness(Status.NOT_FINAL, reason);
    }

    public static PartitionReadiness dependencyPending(String reason) {
        return new PartitionReadiness(Status.DEPENDENCY_PENDING, reason);
    }

    public static PartitionReadiness unavailable(String reason) {
        return new PartitionReadiness(Status.UNAVAILABLE, reason);
    }

    public boolean isReady() {
        return status == Status.READY;
    }
}
