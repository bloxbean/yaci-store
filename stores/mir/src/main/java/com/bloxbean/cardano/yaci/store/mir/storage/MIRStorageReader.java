package com.bloxbean.cardano.yaci.store.mir.storage;

import com.bloxbean.cardano.yaci.store.mir.domain.MirPot;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousRewardSummary;

import java.math.BigInteger;
import java.util.List;

public interface MIRStorageReader {
    List<MoveInstataneousRewardSummary> findMIRSummaries(int page, int count);
    List<MoveInstataneousReward> findMIRsByTxHash(String txHash);

    /**
     * Total MIR amount for a MirPot for a given epoch
     * @param epoch
     * @return Amount
     */
    BigInteger findMirPotAmountByEpoch(int epoch, MirPot mirPot);

}
