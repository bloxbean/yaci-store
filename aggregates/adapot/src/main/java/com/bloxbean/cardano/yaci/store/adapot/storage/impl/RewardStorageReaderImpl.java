package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.InstantReward;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorageReader;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.InstantRewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRepository;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardType;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class RewardStorageReaderImpl implements RewardStorageReader {
    private final InstantRewardRepository instantRewardRepository;
    private final RewardRepository rewardRepository;
    private final Mapper mapper;

    @Override
    public List<InstantReward> findInstantRewardByEarnedEpoch(long epoch, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return instantRewardRepository.findByEarnedEpoch(epoch, sortedBySlot).stream().map(mapper::toInstantReward).toList();
    }

    @Override
    public List<InstantReward> findInstantRewardByEarnedEpochAndType(long epoch, InstantRewardType rewardType, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return instantRewardRepository.findByEarnedEpochAndType(epoch, rewardType, sortedBySlot).stream().map(mapper::toInstantReward).toList();
    }

    @Override
    public BigInteger findTotalInstanceRewardByEarnedEpochAndType(long epoch, InstantRewardType rewardType) {
        return instantRewardRepository.findTotalAmountByEarnedEpoch((int) epoch, rewardType);
    }

    @Override
    public Optional<Integer> getLastRewardCalculationEpoch(RewardType rewardType) {
        return rewardRepository.getLastRewardCalculationEpoch(rewardType);
    }
}
