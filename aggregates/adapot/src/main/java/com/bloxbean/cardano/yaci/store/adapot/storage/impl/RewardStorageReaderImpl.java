package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorageReader;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRepository;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class RewardStorageReaderImpl implements RewardStorageReader {
    private final RewardRepository rewardActivityRepository;
    private final Mapper mapper;

    @Override
    public List<Reward> findByEarnedEpoch(long epoch, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return rewardActivityRepository.findByEarnedEpoch(epoch, sortedBySlot).stream().map(mapper::toReward).toList();
    }

    @Override
    public List<Reward> findByEarnedEpochAndType(long epoch, RewardType rewardType, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return rewardActivityRepository.findByEarnedEpochAndType(epoch, rewardType, sortedBySlot).stream().map(mapper::toReward).toList();
    }

    @Override
    public Optional<Integer> getLastRewardCalculationEpoch(RewardType rewardType) {
        return rewardActivityRepository.getLastRewardCalculationEpoch(rewardType);
    }
}
