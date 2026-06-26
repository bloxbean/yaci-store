package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.enums;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.Constants;

/**
 * Lifecycle of the in-process CIP-26 off-chain registry sync.
 *
 * <p>The current value is exposed through {@code SyncStatus} on
 * {@code Cip26MetadataSyncService} and consumed by
 * {@code OffchainSyncHealthIndicator}, which maps each value to a Spring Boot
 * {@link org.springframework.boot.actuate.health.Health} status used by
 * liveness/readiness probes.
 */
public enum SyncStatusEnum {

    /**
     * Initial value when CIP-26 sync is enabled but the cron has not yet executed
     * its first run (set in {@code Cip26MetadataSyncService#initSyncStatus}). Health
     * indicator maps this to {@code OUT_OF_SERVICE} so readiness probes don't route
     * traffic to a process whose registry cache is empty.
     */
    SYNC_NOT_STARTED(Constants.SYNC_NOT_STARTED),

    /**
     * A sync run is currently executing (clone / diff / persist). Set at the top of
     * {@code synchronizeDatabase()}. Health indicator maps this to
     * {@code OUT_OF_SERVICE} for the same reason as {@link #SYNC_NOT_STARTED} —
     * partial data may be visible mid-run.
     */
    SYNC_IN_PROGRESS(Constants.SYNC_IN_PROGRESS),

    /**
     * Most recent sync run finished successfully (either applied a delta or detected
     * "nothing new since last commit"). Health indicator maps this to {@code UP}.
     */
    SYNC_DONE(Constants.SYNC_DONE),

    /**
     * Most recent sync run threw an exception. Health indicator maps this to
     * {@code DOWN}, distinct from {@code OUT_OF_SERVICE} so probe groups can treat
     * it as alertable rather than "wait it out".
     */
    SYNC_ERROR(Constants.SYNC_ERROR),

    /**
     * In-process CIP-26 sync is disabled ({@code store.assets.ext.cip26.enabled=false}).
     * Set once at startup in {@code initSyncStatus}; never transitions out of this
     * state.
     *
     * <p>The implicit assumption is that the operator is keeping
     * {@code cip26_metadata} fresh via some external mechanism (separate batch job,
     * data pipeline, manual import) — yaci itself has no way to verify whether such
     * a job exists or is working. Health indicator maps this to {@code UP} on that
     * assumption; returning {@code DOWN} would falsely page on every probe when an
     * operator deliberately turns the cron off.
     */
    SYNC_DISABLED(Constants.SYNC_DISABLED);

    private final String text;

    SyncStatusEnum(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

}
