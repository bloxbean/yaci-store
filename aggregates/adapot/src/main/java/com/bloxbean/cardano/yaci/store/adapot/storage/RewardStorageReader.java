package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;

import java.util.List;

public interface RewardStorageReader {
    List<Reward> findByEarnedEpoch(long epoch, int page, int count);

    List<Reward> findByEarnedEpochAndType(long epoch, RewardType rewardType, int page, int count);

}
