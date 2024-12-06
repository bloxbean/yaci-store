package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.InstantReward;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;

import java.util.List;

public interface RewardStorage {
    void saveInstantRewards(List<InstantReward> rewards);

    void saveRewardRest(List<RewardRest> rewards);

    void saveRewards(List<Reward> poolRewards);

    void deleteLeaderMemberRewards(int epoch);

    int deleteBySlotGreaterThan(long slot);
}
