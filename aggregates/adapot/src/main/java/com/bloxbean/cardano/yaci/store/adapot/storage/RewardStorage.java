package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.InstantReward;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.domain.UnclaimedRewardRest;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;

import java.util.List;

public interface RewardStorage {
    void saveInstantRewards(List<InstantReward> rewards);

    void saveRewardRest(List<RewardRest> rewards);

    void saveRewards(List<Reward> poolRewards);

    void bulkSaveRewards(List<Reward> rewards, int batchSize);

    void bulkSaveRewardsWithCopy(List<Reward> rewards, int spendableEpoch);

    void saveUnclaimedRewardRest(List<UnclaimedRewardRest> rewards);

    List<UnclaimedRewardRest> findUnclaimedRewardRest(int spendableEpoch);

    List<RewardRest> findTreasuryWithdrawals(int spendableEpoch);

    int deleteLeaderMemberRewards(int earnedEpoch);

    int deleteRewardRest(int earnedEpoch, RewardRestType type);
    int deleteUnclaimedRewardRest(int earnedEpoch, RewardRestType type);

    int deleteInstantRewardsBySlotGreaterThan(long slot);

    /**
     * Deletes rewards with slot greater than the given slot.
     *
     * @param slot the rollback slot
     * @param fromSpendableEpoch the epoch of {@code slot}. A reward's spendable_epoch is the epoch of its slot,
     *                           so this bound removes no extra rows; it only lets PostgreSQL skip the older partitions.
     */
    int deleteRewardsBySlotGreaterThan(long slot, int fromSpendableEpoch);

    int deleteRewardRestsBySlotGreaterThan(long slot);

    int deleteUnclaimedRewardsBySlotGreaterThan(long slot);
}
