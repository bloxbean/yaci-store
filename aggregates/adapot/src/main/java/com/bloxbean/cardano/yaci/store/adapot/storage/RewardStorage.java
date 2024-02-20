package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;

import java.util.List;

public interface RewardStorage {
    void save(List<Reward> rewards);

    int deleteBySlotGreaterThan(long slot);
}
