package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.snapshot.StakeSnapshotService;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ComponentScan
public class StakesnapshotServiceTest {
    @Autowired
    private StakeSnapshotService stakeSnapshotService;

    @Autowired
    private StoreProperties storeProperties;
    @Autowired
    private DefaultDSLContext dslContext;

    @BeforeEach
    public void setup() {
        storeProperties.setMainnet(true);
        dslContext.execute("delete from epoch_stake");
        dslContext.execute("delete from pool");
        dslContext.execute("delete from delegation");
        dslContext.execute("delete from stake_address_balance");
    }

    @Test
    void shouldExcludePoolWhenRetirementIsEffectiveWithoutRetiredRow() {
        insertDelegationAndBalance("pool1", "stake1", 10, 1_000L);
        insertPoolStatus("pool1", "RETIRING", 9, 100L, 10);

        stakeSnapshotService.takeStakeSnapshot(10);

        assertThat(epochStakeCount(10)).isZero();
    }

    @Test
    void shouldIncludePoolWhenRetirementIsInFuture() {
        insertDelegationAndBalance("pool1", "stake1", 10, 1_000L);
        insertPoolStatus("pool1", "RETIRING", 9, 100L, 11);

        stakeSnapshotService.takeStakeSnapshot(10);

        assertThat(epochStakeCount(10)).isOne();
    }

    @Test
    void shouldIncludePoolWhenLaterUpdateCancelsEffectiveRetirement() {
        insertDelegationAndBalance("pool1", "stake1", 10, 1_000L);
        insertPoolStatus("pool1", "RETIRING", 9, 100L, 10);
        insertPoolStatus("pool1", "UPDATE", 9, 110L, null);

        stakeSnapshotService.takeStakeSnapshot(10);

        assertThat(epochStakeCount(10)).isOne();
    }

//    @Test
    void takeSnapshot() throws IOException {
        dslContext.execute("delete from epoch_stake where epoch >= 207");
        stakeSnapshotService.takeStakeSnapshot(207);
    }

    private void insertDelegationAndBalance(String poolId, String address, int epoch, long amount) {
        dslContext.execute("""
                insert into delegation
                    (tx_hash, cert_index, tx_index, credential, pool_id, address, epoch, slot)
                values (?, 0, 0, ?, ?, ?, ?, 90)
                """, "delegation-" + address, "credential-" + address, poolId, address, epoch - 1);
        dslContext.execute("""
                insert into stake_address_balance (address, slot, quantity, epoch)
                values (?, 80, ?, ?)
                """, address, amount, epoch - 1);
    }

    private void insertPoolStatus(String poolId, String status, int epoch, long slot, Integer retireEpoch) {
        dslContext.execute("""
                insert into pool
                    (pool_id, tx_hash, cert_index, tx_index, status, epoch, retire_epoch,
                     registration_slot, slot)
                values (?, ?, 0, 0, ?, ?, ?, 1, ?)
                """, poolId, status + "-" + slot, status, epoch, retireEpoch, slot);
    }

    private int epochStakeCount(int epoch) {
        return dslContext.fetchCount(
                dslContext.selectFrom(org.jooq.impl.DSL.table("epoch_stake"))
                        .where(org.jooq.impl.DSL.field("epoch").eq(epoch)));
    }


}
