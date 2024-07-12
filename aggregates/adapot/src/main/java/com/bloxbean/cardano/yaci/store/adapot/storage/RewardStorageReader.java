package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.InstantReward;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardType;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public interface RewardStorageReader {
    List<InstantReward> findInstantRewardByEarnedEpoch(long epoch, int page, int count);

    List<InstantReward> findInstantRewardByEarnedEpochAndType(long epoch, InstantRewardType rewardType, int page, int count);

    BigInteger findTotalInstanceRewardByEarnedEpochAndType(long epoch, InstantRewardType rewardType);

    Optional<Integer> getLastRewardCalculationEpoch(RewardType rewardType);
}
