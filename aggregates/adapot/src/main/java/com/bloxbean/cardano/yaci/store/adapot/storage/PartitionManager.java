package com.bloxbean.cardano.yaci.store.adapot.storage;

public interface PartitionManager {
    void ensureRewardPartition(int spendableEpoch);
}
