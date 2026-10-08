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

        // epoch_stake is range-partitioned on epoch, and every row has active_epoch = epoch + 2
        // (genesis rows: 0 or epoch + 1), so active_epoch >= N implies epoch >= N - 2. Without
        // that bound on the partition key the lookup plans and probes every epoch partition.
        assertThat(sql)
                .contains("\"epoch_stake\".\"address\" = 'stake_test1abc'")
                .contains("\"epoch_stake\".\"active_epoch\" >= 1444")
                .contains("\"epoch_stake\".\"epoch\" >= 1442");
    }

    @Test
    void activeEpochStakeCondition_neverBoundsBelowEpochZero() {
        String sql = DSL.using(SQLDialect.POSTGRES).renderInlined(
                BFAccountStorageReaderImpl.activeEpochStakeCondition("stake_test1abc", 1));

        assertThat(sql).contains("\"epoch_stake\".\"epoch\" >= 0");
    }
}
