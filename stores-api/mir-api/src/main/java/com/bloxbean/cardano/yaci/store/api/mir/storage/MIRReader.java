package com.bloxbean.cardano.yaci.store.api.mir.storage;

import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousRewardSummary;

import java.util.List;

public interface MIRReader {
    List<MoveInstataneousRewardSummary> findMIRSummaries(int page, int count);
    List<MoveInstataneousReward> findMIRsByTxHash(String txHash);
}
