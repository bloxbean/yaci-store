package com.bloxbean.cardano.yaci.store.adapot.reward.storage;

import com.bloxbean.cardano.yaci.store.adapot.reward.domain.RewardCalcJob;
import com.bloxbean.cardano.yaci.store.adapot.reward.domain.RewardCalcStatus;

import java.util.List;

public interface RewardCalcJobStorage {

    List<RewardCalcJob> getJobsByStatus(RewardCalcStatus status);

    void save(RewardCalcJob job);
}
