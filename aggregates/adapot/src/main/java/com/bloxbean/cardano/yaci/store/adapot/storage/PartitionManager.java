package com.bloxbean.cardano.yaci.store.adapot.storage;

public interface PartitionManager {
    void ensureRewardPartition(int spendableEpoch);
    void ensureEpochStakePartition(int epoch);
    void ensureDRepDistPartition(int epoch);
}
