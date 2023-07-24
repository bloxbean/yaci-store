package com.bloxbean.cardano.yaci.store.mir.storage;

import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousRewardSummary;

import java.util.List;

public interface MIRStorage {
    void save(List<MoveInstataneousReward> moveInstataneousRewards);
    List<MoveInstataneousRewardSummary> findMIRSummaries(int page, int count);
    List<MoveInstataneousReward> findMIRsByTxHash(String txHash);

    int rollbackMIRs(Long slot);
}
