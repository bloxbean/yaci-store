package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.transaction.domain.Withdrawal;

import java.util.List;

public interface RewardAccountStorage {
    void addReward(List<Reward> rewardAccount);

    void withdrawReward(List<Withdrawal> withdrawals);

    int deleteBySlotGreaterThan(long slot);
}
