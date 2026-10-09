package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl;

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BFAccountStorageReaderImplTest {

    @Test
    void activeEpochStakeCondition_boundsThePartitionKey() {
        String sql = DSL.using(SQLDialect.POSTGRES).renderInlined(
                BFAccountStorageReaderImpl.activeEpochStakeCondition("stake_test1abc", 1444));

        // epoch_stake is range-partitioned on epoch, and both of its writers (the stake snapshot
        // and the genesis pools) store active_epoch = epoch + 2, so active_epoch >= N is the same
        // filter as epoch >= N - 2. Stated on the partition key, the lookup no longer plans and
        // probes every epoch partition.
        assertThat(sql)
                .contains("\"epoch_stake\".\"address\" = 'stake_test1abc'")
                .contains("\"epoch_stake\".\"epoch\" >= 1442")
                .doesNotContain("active_epoch");
    }

    @Test
    void activeEpochStakeCondition_keepsGenesisRowsAtEpochMinusOne() {
        String sql = DSL.using(SQLDialect.POSTGRES).renderInlined(
                BFAccountStorageReaderImpl.activeEpochStakeCondition("stake_test1abc", 1));

        // GenesisPoolProcessor writes a devnet's genesis delegators at epoch = -1 (active_epoch = 1),
        // so the bound must go negative rather than clamp at 0 and skip them.
        assertThat(sql).contains("\"epoch_stake\".\"epoch\" >= -1");
    }
}
