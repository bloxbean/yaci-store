package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.*;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardType;

import java.math.BigInteger;
import java.util.List;

public interface RewardStorageReader {
    //Instant Rewards
    List<InstantReward> findInstantRewardByEarnedEpoch(Integer epoch, int page, int count);
    List<InstantReward> findInstantRewardByEarnedEpochAndType(Integer epoch, InstantRewardType rewardType, int page, int count);
    BigInteger findTotalInstantRewardByEarnedEpochAndType(Integer epoch, InstantRewardType rewardType);

    //Pool Rewards
    List<Reward> findRewardsByEarnedEpoch(Integer epoch, int page, int count);
    List<Reward> findRewardsBySpendableEpoch(Integer epoch, int page, int count);

    List<Reward> findRewardsByAddress(String address, int page, int count, Order order);
    List<Reward> findRewardsByAddressAndEarnedEpoch(String address, Integer epoch);
    List<Reward> findRewardsByAddressAndSpendableEpoch(String address, Integer epoch);

    //Reward Rest
    List<RewardRest> findRewardRestByEarnedEpoch(Integer epoch, int page, int count);
    List<RewardRest> findRewardRestBySpendableEpoch(Integer epoch, int page, int count);

    List<RewardRest> findRewardRestByAddress(String address, int page, int count, Order order);
    List<RewardRest> findRewardRestByAddressAndEarnedEpoch(String address, Integer earnedEpoch);
    List<RewardRest> findRewardRestByAddressAndSpendableEpoch(String address, Integer spendableEpoch);

    //Unclaimed Reward Rest
    List<UnclaimedRewardRest> findUnclaimedRewardRestByEarnedEpoch(Integer epoch, int page, int count, Order order);
    List<UnclaimedRewardRest> findUnclaimedRewardRestBySpendableEpoch(Integer epoch, int page, int count, Order order);

    List<Reward> findRewardsByPoolHashAndSpendableEpoch(String poolHash, Integer spendableEpoch, int page, int count);

    //Unwithdrawn Rewards
    List<RewardInfo> findUnwithdrawnRewardsByAddresses(List<String> addresses, int page, int count);
}
