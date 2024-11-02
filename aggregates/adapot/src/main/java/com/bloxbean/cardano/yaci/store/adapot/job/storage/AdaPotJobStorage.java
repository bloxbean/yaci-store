package com.bloxbean.cardano.yaci.store.adapot.job.storage;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;

import java.util.List;

public interface AdaPotJobStorage {

    List<AdaPotJob> getJobsByTypeAndStatus(AdaPotJobType type, AdaPotJobStatus status);

    void save(AdaPotJob job);

    int deleteBySlotGreaterThan(long slot);
}
