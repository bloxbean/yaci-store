package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorageReader;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "store.adapot.api-enabled=true")
class RewardStorageReaderImplTest {

    private static final String ADDRESS = "stake_test1up97ct2wt8jqlly2cnkhuwc7tvevmjpp7h6ts3rucpksy8c8cnspn";
    private static final String FULLY_WITHDRAWN_ADDRESS = "stake_test1uzzd5vuumkaq2c3a95gyt7rf28pdna4zpza9vjxmp7w00yc2rp5he";

    @Autowired
    private RewardStorageReader rewardStorageReader;

    @Autowired
    private DefaultDSLContext dsl;

    @BeforeEach
    void setup() {
        dsl.deleteFrom(org.jooq.impl.DSL.table("reward")).execute();
        dsl.deleteFrom(org.jooq.impl.DSL.table("reward_rest")).execute();
        dsl.deleteFrom(org.jooq.impl.DSL.table("instant_reward")).execute();
        dsl.deleteFrom(org.jooq.impl.DSL.table("withdrawal")).execute();
        dsl.deleteFrom(org.jooq.impl.DSL.table("epoch_param")).execute();
        dsl.deleteFrom(org.jooq.impl.DSL.table("block")).execute();
    }

    // A withdrawal clears earlier rewards; only spendable rewards after the latest withdrawal are withdrawable.
    @Test
    void findWithdrawableRewardByAddressReturnsSpendableRewardsAfterLastWithdrawal() {
        insertCurrentState(10, 200);

        dsl.execute("""
                insert into reward (address, earned_epoch, type, pool_id, amount, spendable_epoch, slot)
                values (?, 8, 'member', 'pool1', 100, 9, 90)
                """, ADDRESS);
        dsl.execute("""
                insert into reward_rest (id, address, type, amount, earned_epoch, spendable_epoch, slot)
                values (?, ?, 'treasury', 20, 9, 10, 100)
                """, java.util.UUID.randomUUID(), ADDRESS);
        dsl.execute("""
                insert into instant_reward (address, type, amount, earned_epoch, spendable_epoch, slot)
                values (?, 'reserves', 30, 9, 10, 101)
                """, ADDRESS);
        dsl.execute("""
                insert into reward (address, earned_epoch, type, pool_id, amount, spendable_epoch, slot)
                values (?, 10, 'leader', 'pool2', 999, 11, 110)
                """, ADDRESS);
        dsl.execute("""
                insert into withdrawal (address, tx_hash, amount, epoch, slot)
                values (?, 'tx1', 150, 10, 120)
                """, ADDRESS);
        dsl.execute("""
                insert into reward_rest (id, address, type, amount, earned_epoch, spendable_epoch, slot)
                values (?, ?, 'treasury', 40, 10, 10, 130)
                """, java.util.UUID.randomUUID(), ADDRESS);
        dsl.execute("""
                insert into instant_reward (address, type, amount, earned_epoch, spendable_epoch, slot)
                values (?, 'reserves', 999, 10, 11, 140)
                """, ADDRESS);

        var withdrawableReward = rewardStorageReader.findWithdrawableRewardByAddress(ADDRESS);

        assertThat(withdrawableReward.getAddress()).isEqualTo(ADDRESS);
        assertThat(withdrawableReward.getWithdrawableAmount()).isEqualTo(BigInteger.valueOf(40));
        assertThat(withdrawableReward.getEpoch()).isEqualTo(10);
        assertThat(withdrawableReward.getSlot()).isEqualTo(200);
    }

    // A withdrawal resets the reward account at its slot; rewards recorded later remain withdrawable.
    @Test
    void findWithdrawableRewardByAddressTreatsWithdrawalSlotAsResetBoundary() {
        insertCurrentState(10, 200);
        dsl.execute("""
                insert into withdrawal (address, tx_hash, amount, epoch, slot)
                values (?, 'tx3', 1000, 10, 120)
                """, ADDRESS);
        dsl.execute("""
                insert into reward_rest (id, address, type, amount, earned_epoch, spendable_epoch, slot)
                values (?, ?, 'treasury', 40, 10, 10, 130)
                """, java.util.UUID.randomUUID(), ADDRESS);

        var withdrawableReward = rewardStorageReader.findWithdrawableRewardByAddress(ADDRESS);

        assertThat(withdrawableReward.getWithdrawableAmount()).isEqualTo(BigInteger.valueOf(40));
    }

    // Rewards at or before the latest withdrawal slot are no longer withdrawable.
    @Test
    void findWithdrawableRewardByAddressReturnsZeroWhenRewardsAreFullyWithdrawn() {
        insertCurrentState(10, 200);
        dsl.execute("""
                insert into reward (address, earned_epoch, type, pool_id, amount, spendable_epoch, slot)
                values (?, 8, 'member', 'pool1', 25, 9, 90)
                """, FULLY_WITHDRAWN_ADDRESS);
        dsl.execute("""
                insert into withdrawal (address, tx_hash, amount, epoch, slot)
                values (?, 'tx2', 25, 10, 120)
                """, FULLY_WITHDRAWN_ADDRESS);

        var withdrawableReward = rewardStorageReader.findWithdrawableRewardByAddress(FULLY_WITHDRAWN_ADDRESS);

        assertThat(withdrawableReward.getWithdrawableAmount()).isEqualTo(BigInteger.ZERO);
    }

    // Current lookup reflects the latest withdrawal and does not expose historical reward state.
    @Test
    void findWithdrawableRewardByAddressReturnsCurrentRewardsOnly() {
        insertCurrentState(11, 200);
        dsl.execute("""
                insert into reward (address, earned_epoch, type, pool_id, amount, spendable_epoch, slot)
                values (?, 9, 'member', 'pool1', 40, 10, 130)
                """, ADDRESS);
        dsl.execute("""
                insert into withdrawal (address, tx_hash, amount, epoch, slot)
                values (?, 'tx4', 40, 11, 140)
                """, ADDRESS);

        var currentReward = rewardStorageReader.findWithdrawableRewardByAddress(ADDRESS);

        assertThat(currentReward.getWithdrawableAmount()).isEqualTo(BigInteger.ZERO);
        assertThat(currentReward.getEpoch()).isEqualTo(11);
        assertThat(currentReward.getSlot()).isEqualTo(200);
    }

    // An address without reward rows should return a zero withdrawable amount.
    @Test
    void findWithdrawableRewardByAddressReturnsZeroWhenAddressHasNoRewards() {
        var withdrawableReward = rewardStorageReader.findWithdrawableRewardByAddress("stake_test1no_rewards");

        assertThat(withdrawableReward.getWithdrawableAmount()).isEqualTo(BigInteger.ZERO);
    }

    private void insertCurrentState(int epoch, long slot) {
        dsl.execute("insert into epoch_param (epoch) values (?)", epoch);
        dsl.execute("insert into block (hash, number, epoch, slot, no_of_txs) values (?, 1, ?, ?, 0)",
                "block_" + epoch + "_" + slot, epoch, slot);
    }
}
