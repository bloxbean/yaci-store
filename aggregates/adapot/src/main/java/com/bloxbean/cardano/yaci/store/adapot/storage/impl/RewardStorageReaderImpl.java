package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.InstantReward;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.domain.UnclaimedRewardRest;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorageReader;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.InstantRewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRestRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.UnclaimedRewardRestRepository;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigInteger;
import java.util.List;

@RequiredArgsConstructor
public class RewardStorageReaderImpl implements RewardStorageReader {
    private final InstantRewardRepository instantRewardRepository;
    private final RewardRepository rewardRepository;
    private final RewardRestRepository rewardRestRepository;
    private final UnclaimedRewardRestRepository unclaimedRewardRestRepository;
    private final Mapper mapper;

    @Override
    public List<InstantReward> findInstantRewardByEarnedEpoch(Integer epoch, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return instantRewardRepository.findByEarnedEpoch(epoch, sortedBySlot).stream().map(mapper::toInstantReward).toList();
    }

    @Override
    public List<InstantReward> findInstantRewardByEarnedEpochAndType(Integer epoch, InstantRewardType rewardType, int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return instantRewardRepository.findByEarnedEpochAndType(epoch, rewardType, sortedBySlot).stream().map(mapper::toInstantReward).toList();
    }

    @Override
    public BigInteger findTotalInstanceRewardByEarnedEpochAndType(Integer epoch, InstantRewardType rewardType) {
        return instantRewardRepository.findTotalAmountByEarnedEpoch(epoch, rewardType);
    }

    @Override
    public List<Reward> findRewardsByEarnedEpoch(Integer epoch, int page, int count) {
        Pageable pagable =
                PageRequest.of(page, count);

        return rewardRepository.findByEarnedEpoch(epoch, pagable)
                .stream()
                .map(rewardEntity -> mapper.toReward(rewardEntity))
                .toList();
    }

    @Override
    public List<Reward> findRewardsBySpendableEpoch(Integer epoch, int page, int count) {
        Pageable pagable =
                PageRequest.of(page, count);

        return rewardRepository.findBySpendableEpoch(epoch, pagable)
                .stream()
                .map(rewardEntity -> mapper.toReward(rewardEntity))
                .toList();
    }

    @Override
    public List<Reward> findRewardsByAddress(String address, int page, int count, Order order) {
        Pageable sortedBySlot =
                getPagableBySlotOrder(page, count, order);

        return rewardRepository.findByAddress(address, sortedBySlot)
                .stream()
                .map(rewardEntity -> mapper.toReward(rewardEntity))
                .toList();
    }

    @Override
    public List<Reward> findRewardsByAddressAndEarnedEpoch(String address, Integer epoch) {
        return rewardRepository.findByAddressAndEarnedEpoch(address, epoch)
                .stream()
                .map(rewardEntity -> mapper.toReward(rewardEntity))
                .toList();
    }

    @Override
    public List<Reward> findRewardsByAddressAndSpendableEpoch(String address, Integer epoch) {
        return rewardRepository.findByAddressAndSpendableEpoch(address, epoch)
                .stream()
                .map(rewardEntity -> mapper.toReward(rewardEntity))
                .toList();
    }

    //-- Reward Rest
    @Override
    public List<RewardRest> findRewardRestByEarnedEpoch(Integer epoch, int page, int count) {
        Pageable pagable =
                PageRequest.of(page, count);

        return rewardRestRepository.findByEarnedEpoch(epoch, pagable)
                .stream()
                .map(rewardEntity -> mapper.toRewardRest(rewardEntity))
                .toList();
    }

    @Override
    public List<RewardRest> findRewardRestBySpendableEpoch(Integer epoch, int page, int count) {
        Pageable pagable =
                PageRequest.of(page, count);

        return rewardRestRepository.findBySpendableEpoch(epoch, pagable)
                .stream()
                .map(rewardRestEntity -> mapper.toRewardRest(rewardRestEntity))
                .toList();
    }

    @Override
    public List<RewardRest> findRewardRestByAddress(String address, int page, int count, Order order) {
        Pageable sortedBySlot = getPagableBySlotOrder(page, count, order);

        return rewardRestRepository.findByAddress(address, sortedBySlot)
                .stream()
                .map(rewardEntity -> mapper.toRewardRest(rewardEntity))
                .toList();
    }

    @Override
    public List<RewardRest> findRewardRestByAddressAndEarnedEpoch(String address, Integer earnedEpoch) {
        return rewardRestRepository.findByAddressAndEarnedEpoch(address, earnedEpoch)
                .stream()
                .map(rewardEntity -> mapper.toRewardRest(rewardEntity))
                .toList();
    }

    @Override
    public List<RewardRest> findRewardRestByAddressAndSpendableEpoch(String address, Integer spendableEpoch) {
        return rewardRestRepository.findByAddressAndSpendableEpoch(address, spendableEpoch)
                .stream()
                .map(rewardEntity -> mapper.toRewardRest(rewardEntity))
                .toList();
    }

    //-- Unclaimed Reward Rest
    @Override
    public List<UnclaimedRewardRest> findUnclaimedRewardRestByEarnedEpoch(Integer epoch, int page, int count, Order order) {
        Pageable sortedBySlot = getPagableBySlotOrder(page, count, order);

        return unclaimedRewardRestRepository.findByEarnedEpoch(epoch, sortedBySlot)
                .stream()
                .map(rewardEntity -> mapper.toUnclaimedRewardRest(rewardEntity))
                .toList();
    }

    @Override
    public List<UnclaimedRewardRest> findUnclaimedRewardRestBySpendableEpoch(Integer epoch, int page, int count, Order order) {
        Pageable sortedBySlot = getPagableBySlotOrder(page, count, order);

        return unclaimedRewardRestRepository.findBySpendableEpoch(epoch, sortedBySlot)
                .stream()
                .map(rewardEntity -> mapper.toUnclaimedRewardRest(rewardEntity))
                .toList();
    }

    @Override
    public List<Reward> findRewardsByPoolHashAndSpendableEpoch(String poolHash, Integer spendableEpoch, int page, int count) {
        Pageable pagable =
                PageRequest.of(page, count);

        return rewardRepository.findByPoolIdAndSpendableEpoch(poolHash, spendableEpoch, pagable)
                .stream()
                .map(rewardEntity -> mapper.toReward(rewardEntity))
                .toList();
    }


    private static Pageable getPagableBySlotOrder(int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count,  order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());
        return sortedBySlot;
    }

}
