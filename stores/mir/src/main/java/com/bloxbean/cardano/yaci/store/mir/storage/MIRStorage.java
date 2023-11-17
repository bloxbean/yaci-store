package com.bloxbean.cardano.yaci.store.mir.storage;

import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;

import java.util.List;

public interface MIRStorage {
    void save(List<MoveInstataneousReward> moveInstataneousRewards);

    int rollbackMIRs(Long slot);
}
