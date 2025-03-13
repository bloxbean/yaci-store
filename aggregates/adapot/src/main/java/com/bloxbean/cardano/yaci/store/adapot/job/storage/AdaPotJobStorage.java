package com.bloxbean.cardano.yaci.store.adapot.job.storage;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;

import java.util.List;
import java.util.Optional;

public interface AdaPotJobStorage {

    List<AdaPotJob> getJobsByTypeAndStatus(AdaPotJobType type, AdaPotJobStatus status);

    Optional<AdaPotJob> getJobByEpoch(int epoch);

    void save(AdaPotJob job);

    int deleteBySlotGreaterThan(long slot);
}
