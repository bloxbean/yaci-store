package com.bloxbean.cardano.yaci.store.adapot.job.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class AdaPotJobStorageImpl implements AdaPotJobStorage {

    private final AdaPotJobRepository adaPotJobRepository;
    private final AdaPotJobMapper mapper;

    @Override
    public List<AdaPotJob> getJobsByTypeAndStatus(AdaPotJobType type, AdaPotJobStatus status) {
        return adaPotJobRepository.findByTypeAndStatusOrderByEpoch(type, status)
                .stream()
                .map(rewardCalcJobEntity -> mapper.toDomain(rewardCalcJobEntity))
                .toList();
    }

    @Override
    public Optional<AdaPotJob> getJobByEpoch(int epoch) {
        return adaPotJobRepository.findByEpoch(epoch)
                .map(mapper::toDomain);
    }

    @Override
    public void save(AdaPotJob job) {
        adaPotJobRepository.save(mapper.toEntity(job));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return adaPotJobRepository.deleteBySlotGreaterThan(slot);
    }
}
