package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.storage.PartitionManager;
import lombok.extern.slf4j.Slf4j;

/**
 * No-op implementation of PartitionManager for databases that don't support partitioning
 * (MySQL, H2, etc.).
 *
 * This implementation does nothing and is used when the database is not PostgreSQL.
 * For PostgreSQL databases, {@link PostgresPartitionManager} is used instead.
 */
@Slf4j
public class NoOpPartitionManager implements PartitionManager {

    @Override
    public void ensureRewardPartition(int spendableEpoch) {
        // No-op - partitioning is not supported for non-PostgreSQL databases
        log.trace("Reward partition creation skipped - not using PostgreSQL");
    }

    @Override
    public void ensureEpochStakePartition(int epoch) {
        // No-op - partitioning is not supported for non-PostgreSQL databases
        log.trace("Epoch stake partition creation skipped - not using PostgreSQL");
    }

    @Override
    public void ensureDRepDistPartition(int epoch) {
        // No-op - partitioning is not supported for non-PostgreSQL databases
        log.trace("DRep dist partition creation skipped - not using PostgreSQL");
    }
}