package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class RewardStorageImpl implements RewardStorage {
    private final RewardRepository rewardRepository;
    private final Mapper mapper;

    @Override
    public void save(List<Reward> rewards) {
        rewardRepository.saveAll(rewards.stream().map(mapper::toRewardEntity).toList());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return rewardRepository.deleteBySlotGreaterThan(slot);
    }
}
