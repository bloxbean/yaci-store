package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * No-op implementation used when adapot module is disabled (store.adapot.enabled=false).
 */
@Slf4j
public class NoOpAdaPotJobStorage implements AdaPotJobStorage {

    public NoOpAdaPotJobStorage() {
        log.warn("AdaPot module is disabled (store.adapot.enabled=false). " +
                 "Epoch-partitioned exports that depend on AdaPot job completion will be skipped.");
    }

    @Override
    public List<AdaPotJob> getJobsByTypeAndStatus(AdaPotJobType type, AdaPotJobStatus status) {
        return Collections.emptyList();
    }

    @Override
    public Optional<AdaPotJob> getJobByTypeAndEpoch(AdaPotJobType type, int epoch) {
        return Optional.empty();
    }

    @Override
    public void save(AdaPotJob job) {
        // no-op
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return 0;
    }

    @Override
    public Optional<AdaPotJob> getLatestJobByTypeAndStatus(AdaPotJobType type, AdaPotJobStatus status) {
        return Optional.empty();
    }

    @Override
    public List<AdaPotJob> getRecentCompletedJobs(int limit) {
        return Collections.emptyList();
    }
}
