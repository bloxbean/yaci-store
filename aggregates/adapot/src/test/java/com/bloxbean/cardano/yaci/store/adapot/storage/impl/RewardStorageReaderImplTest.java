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
    }

    @Test
    void findWithdrawableRewardByAddressReturnsSpendableRewardsMinusWithdrawals() {
        dsl.execute("insert into epoch_param (epoch) values (10)");

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

        var withdrawableReward = rewardStorageReader.findWithdrawableRewardByAddress(ADDRESS);

        assertThat(withdrawableReward.getAddress()).isEqualTo(ADDRESS);
        assertThat(withdrawableReward.getWithdrawableAmount()).isEqualTo(BigInteger.valueOf(40));
    }

    @Test
    void findWithdrawableRewardByAddressReturnsZeroWhenRewardsAreFullyWithdrawn() {
        dsl.execute("insert into epoch_param (epoch) values (10)");
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

    @Test
    void findWithdrawableRewardByAddressReturnsZeroWhenAddressHasNoRewards() {
        var withdrawableReward = rewardStorageReader.findWithdrawableRewardByAddress("stake_test1no_rewards");

        assertThat(withdrawableReward.getWithdrawableAmount()).isEqualTo(BigInteger.ZERO);
    }
}
