package com.bloxbean.cardano.yaci.store.adapot.job.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

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
    public Optional<AdaPotJob> getJobByTypeAndEpoch(AdaPotJobType type, int epoch) {
        return adaPotJobRepository.findByTypeAndEpoch(type, epoch)
                .map(mapper::toDomain);
    }

    @Transactional
    @Override
    public void save(AdaPotJob job) {
        adaPotJobRepository.save(mapper.toEntity(job));
    }

    @Transactional
    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return adaPotJobRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public Optional<AdaPotJob> getLatestJobByTypeAndStatus(AdaPotJobType type, AdaPotJobStatus status) {
        return adaPotJobRepository.findTopByTypeAndStatusOrderByEpochDesc(type, status)
                .map(mapper::toDomain);
    }

    @Override
    public List<AdaPotJob> getRecentCompletedJobs(int limit) {
        return adaPotJobRepository.findRecentJobsByTypeAndStatus(AdaPotJobType.REWARD_CALC, AdaPotJobStatus.COMPLETED, Pageable.ofSize(limit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
