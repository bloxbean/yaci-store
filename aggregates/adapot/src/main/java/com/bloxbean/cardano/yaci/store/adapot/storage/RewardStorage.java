package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.InstantReward;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.domain.UnclaimedRewardRest;

import java.util.List;

public interface RewardStorage {
    void saveInstantRewards(List<InstantReward> rewards);

    void saveRewardRest(List<RewardRest> rewards);

    void saveRewards(List<Reward> poolRewards);

    void bulkSaveRewards(List<Reward> rewards, int batchSize);

    void saveUnclaimedRewardRest(List<UnclaimedRewardRest> rewards);

    List<UnclaimedRewardRest> findUnclaimedRewardRest(int spendableEpoch);

    void deleteLeaderMemberRewards(int epoch);

    int deleteInstantRewardsBySlotGreaterThan(long slot);

    int deleteRewardsBySlotGreaterThan(long slot);

    int deleteRewardRestsBySlotGreaterThan(long slot);

    int deleteUnclaimedRewardsBySlotGreaterThan(long slot);
}
