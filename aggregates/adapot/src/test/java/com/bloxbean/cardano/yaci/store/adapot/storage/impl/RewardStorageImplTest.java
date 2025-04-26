package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.MapperImpl;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardRestEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.UnclaimedRewardRestEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.InstantRewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRestRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.UnclaimedRewardRestRepository;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

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

    private RewardStorage rewardStorage;

    @BeforeEach
    public void setup() {
        rewardStorage = new RewardStorageImpl(instantRewardRepository, rewardRestRepository, rewardRepository, unclaimedRewardRestRepository, new MapperImpl(), null);
    }

    @Test
    void findTreasuryWithdrawals() {
        RewardRestEntity rewardRestEntity1 = RewardRestEntity.builder()
                .address("stake_test1urqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y4p")
                .amount(adaToLovelace(100000))
                .type(RewardRestType.proposal_refund)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        RewardRestEntity rewardRestEntity2= RewardRestEntity.builder()
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(20))
                .type(RewardRestType.treasury)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        RewardRestEntity rewardRestEntity3= RewardRestEntity.builder()
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(10))
                .type(RewardRestType.treasury)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        RewardRestEntity rewardRestEntity4= RewardRestEntity.builder()
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
    void deleteRewardRest() {
        RewardRestEntity rewardRestEntity1 = RewardRestEntity.builder()
                .address("stake_test1urqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y4p")
                .amount(adaToLovelace(100000))
                .type(RewardRestType.proposal_refund)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        RewardRestEntity rewardRestEntity2= RewardRestEntity.builder()
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(20))
                .type(RewardRestType.treasury)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        RewardRestEntity rewardRestEntity3= RewardRestEntity.builder()
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(10))
                .type(RewardRestType.treasury)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        RewardRestEntity rewardRestEntity4= RewardRestEntity.builder()
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(90))
                .type(RewardRestType.treasury)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        RewardRestEntity rewardRestEntity5= RewardRestEntity.builder()
                .address("stake_test1dwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(70))
                .type(RewardRestType.treasury)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        RewardRestEntity rewardRestEntity6= RewardRestEntity.builder()
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
                .address("stake_test1urqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y4p")
                .amount(adaToLovelace(100000))
                .type(RewardRestType.proposal_refund)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        UnclaimedRewardRestEntity rewardRestEntity2= UnclaimedRewardRestEntity.builder()
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(20))
                .type(RewardRestType.treasury)
                .earnedEpoch(5)
                .spendableEpoch(5)
                .build();

        UnclaimedRewardRestEntity rewardRestEntity3= UnclaimedRewardRestEntity.builder()
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(10))
                .type(RewardRestType.treasury)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        UnclaimedRewardRestEntity rewardRestEntity4= UnclaimedRewardRestEntity.builder()
                .address("stake_test1vwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(90))
                .type(RewardRestType.treasury)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        UnclaimedRewardRestEntity rewardRestEntity5= UnclaimedRewardRestEntity.builder()
                .address("stake_test1dwqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y41")
                .amount(adaToLovelace(70))
                .type(RewardRestType.treasury)
                .earnedEpoch(3)
                .spendableEpoch(4)
                .build();

        UnclaimedRewardRestEntity rewardRestEntity6= UnclaimedRewardRestEntity.builder()
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
