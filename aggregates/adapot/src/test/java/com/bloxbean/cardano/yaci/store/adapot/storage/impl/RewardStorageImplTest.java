package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.AdaPotProperties;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.MapperImpl;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardRestEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.UnclaimedRewardRestEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.InstantRewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRestRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.UnclaimedRewardRestRepository;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.math.BigInteger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RewardStorageImplTest {

    @Autowired
    private InstantRewardRepository instantRewardRepository;
    @Autowired
    private RewardRestRepository rewardRestRepository;
    @Autowired
    private RewardRepository rewardRepository;
    @Autowired
    private UnclaimedRewardRestRepository unclaimedRewardRestRepository;
    @Autowired
    private DataSource dataSource;

    private RewardStorage rewardStorage;

    @BeforeEach
    public void setup() {
        rewardStorage = new RewardStorageImpl(instantRewardRepository, rewardRestRepository, rewardRepository, unclaimedRewardRestRepository, new MapperImpl(), null, new AdaPotProperties());
    }

    @Test
    void findTreasuryWithdrawals() {
        RewardRestEntity rewardRestEntity1 = RewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1urqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y4p")
                .amount(adaToLovelace(100000))
                .type(RewardRestType.proposal_refund)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        RewardRestEntity rewardRestEntity2= RewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(20))
                .type(RewardRestType.treasury)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        RewardRestEntity rewardRestEntity3= RewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(10))
                .type(RewardRestType.treasury)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        RewardRestEntity rewardRestEntity4= RewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(90))
                .type(RewardRestType.treasury)
                .earnedEpoch(3)
                .spendableEpoch(3)
                .build();

        //save data
        rewardRestRepository.saveAll(List.of(rewardRestEntity1, rewardRestEntity2, rewardRestEntity3,rewardRestEntity4));

        List<RewardRest> treasuryWithdrawals = rewardStorage.findTreasuryWithdrawals(5);

        assertThat(treasuryWithdrawals).hasSize(2);
        assertThat(treasuryWithdrawals.get(0).getAmount()).isEqualTo(adaToLovelace(20));
        assertThat(treasuryWithdrawals.get(1).getAmount()).isEqualTo(adaToLovelace(10));
    }

    @Test
    void deleteRewardsBySlotGreaterThan() {
        rewardRepository.saveAllAndFlush(List.of(
                reward("stake_test1_kept", 5, 6, 600L),
                reward("stake_test1_deleted_1", 6, 7, 700L),
                reward("stake_test1_deleted_2", 7, 8, 800L)));

        // TransactionAwareDataSourceProxy: jOOQ runs in the test transaction and sees the saved rows
        var dsl = DSL.using(new TransactionAwareDataSourceProxy(dataSource), SQLDialect.H2,
                new Settings().withRenderQuotedNames(RenderQuotedNames.NEVER));
        var storage = new RewardStorageImpl(instantRewardRepository, rewardRestRepository, rewardRepository,
                unclaimedRewardRestRepository, new MapperImpl(), dsl, new AdaPotProperties());

        int count = storage.deleteRewardsBySlotGreaterThan(600L, 6);

        assertThat(count).isEqualTo(2);
        assertThat(rewardRepository.findAll()).extracting(RewardEntity::getAddress)
                .containsExactly("stake_test1_kept");
    }

    @Test
    void deleteRewardsBySlotGreaterThan_inlinesTheEpochForPartitionPruning() {
        List<String> sqls = new ArrayList<>();
        List<List<Object>> bindings = new ArrayList<>();
        var dsl = DSL.using(new MockConnection(ctx -> {
            sqls.add(ctx.sql());
            bindings.add(Arrays.asList(ctx.bindings()));
            return new MockResult[]{new MockResult(0)};
        }), SQLDialect.POSTGRES);
        var storage = new RewardStorageImpl(instantRewardRepository, rewardRestRepository, rewardRepository,
                unclaimedRewardRestRepository, new MapperImpl(), dsl, new AdaPotProperties());

        storage.deleteRewardsBySlotGreaterThan(119084867L, 320);

        assertThat(sqls).singleElement().asString()
                .contains("\"slot\" > ?")
                .contains("\"spendable_epoch\" >= 320");
        assertThat(bindings).containsExactly(List.of(119084867L));
    }

    private static RewardEntity reward(String address, int earnedEpoch, int spendableEpoch, long slot) {
        return RewardEntity.builder()
                .address(address)
                .earnedEpoch(earnedEpoch)
                .spendableEpoch(spendableEpoch)
                .type(RewardType.member)
                .poolId("pool1")
                .amount(BigInteger.TEN)
                .slot(slot)
                .build();
    }

    @Test
    void deleteRewardRest() {
        RewardRestEntity rewardRestEntity1 = RewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1urqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y4p")
                .amount(adaToLovelace(100000))
                .type(RewardRestType.proposal_refund)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        RewardRestEntity rewardRestEntity2= RewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(20))
                .type(RewardRestType.treasury)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        RewardRestEntity rewardRestEntity3= RewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(10))
                .type(RewardRestType.treasury)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        RewardRestEntity rewardRestEntity4= RewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(90))
                .type(RewardRestType.treasury)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        RewardRestEntity rewardRestEntity5= RewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1dwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(70))
                .type(RewardRestType.treasury)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        RewardRestEntity rewardRestEntity6= RewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1ewqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(80))
                .type(RewardRestType.proposal_refund)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        //save data
        rewardRestRepository.saveAll(List.of(rewardRestEntity1, rewardRestEntity2, rewardRestEntity3,rewardRestEntity4, rewardRestEntity5, rewardRestEntity6));

        int count = rewardStorage.deleteRewardRest(3, RewardRestType.treasury);

        var remainingRewards = rewardRestRepository.findAll();

        assertThat(count).isEqualTo(3);
        assertThat(remainingRewards).hasSize(3);
        assertThat(remainingRewards.stream().filter(rewardEntity -> rewardEntity.getEarnedEpoch() == 3).toList()).hasSize(1);
        assertThat(remainingRewards.stream().filter(rewardEntity -> rewardEntity.getEarnedEpoch() == 3)
                .map(rewardRestEntity -> rewardRestEntity.getType())
                .findFirst().orElse(null))
                .isEqualTo(RewardRestType.proposal_refund);
    }

    @Test
    void deleteUnclaimedRewardRest() {
        UnclaimedRewardRestEntity rewardRestEntity1 = UnclaimedRewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1urqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y4p")
                .amount(adaToLovelace(100000))
                .type(RewardRestType.proposal_refund)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        UnclaimedRewardRestEntity rewardRestEntity2= UnclaimedRewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(20))
                .type(RewardRestType.treasury)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        UnclaimedRewardRestEntity rewardRestEntity3= UnclaimedRewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(10))
                .type(RewardRestType.treasury)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        UnclaimedRewardRestEntity rewardRestEntity4= UnclaimedRewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(90))
                .type(RewardRestType.treasury)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        UnclaimedRewardRestEntity rewardRestEntity5= UnclaimedRewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1dwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(70))
                .type(RewardRestType.treasury)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        UnclaimedRewardRestEntity rewardRestEntity6= UnclaimedRewardRestEntity.builder()
                .id(UUID.randomUUID())
                .address("stake_test1ewqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(80))
                .type(RewardRestType.proposal_refund)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        //save data
        unclaimedRewardRestRepository.saveAll(List.of(rewardRestEntity1, rewardRestEntity2, rewardRestEntity3,rewardRestEntity4, rewardRestEntity5, rewardRestEntity6));

        int count = rewardStorage.deleteUnclaimedRewardRest(3, RewardRestType.treasury);

        var remainingRewards = unclaimedRewardRestRepository.findAll();

        assertThat(count).isEqualTo(3);
        assertThat(remainingRewards).hasSize(3);
        assertThat(remainingRewards.stream().filter(rewardEntity -> rewardEntity.getEarnedEpoch() == 3).toList()).hasSize(1);
        assertThat(remainingRewards.stream().filter(rewardEntity -> rewardEntity.getEarnedEpoch() == 3)
                .map(rewardRestEntity -> rewardRestEntity.getType())
                .findFirst().orElse(null))
                .isEqualTo(RewardRestType.proposal_refund);
    }
}
