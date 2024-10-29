package com.bloxbean.cardano.yaci.store.adapot.reward.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.reward.domain.RewardCalcJob;
import com.bloxbean.cardano.yaci.store.adapot.reward.domain.RewardCalcStatus;
import com.bloxbean.cardano.yaci.store.adapot.reward.storage.RewardCalcJobStorage;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class RewardCalcJobStorageImpl implements RewardCalcJobStorage {

    private final RewardCalcJobRepository rewardCalcJobRepository;
    private final RewardCalcJobMapper mapper;

    @Override
    public List<RewardCalcJob> getJobsByStatus(RewardCalcStatus status) {
        return rewardCalcJobRepository.findByStatusOrderByEpoch(status)
                .stream()
                .map(rewardCalcJobEntity -> mapper.toDomain(rewardCalcJobEntity))
                .toList();
    }

    @Override
    public void save(RewardCalcJob job) {
        rewardCalcJobRepository.save(mapper.toEntity(job));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return rewardCalcJobRepository.deleteBySlotGreaterThan(slot);
    }
}
