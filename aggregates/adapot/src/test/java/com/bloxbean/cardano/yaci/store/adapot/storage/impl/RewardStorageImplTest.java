package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.MapperImpl;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardRestEntity;
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

    private RewardStorage rewardStorage;

    @BeforeEach
    public void setup() {
        rewardStorage = new RewardStorageImpl(instantRewardRepository, rewardRestRepository, rewardRepository, unclaimedRewardRestRepository, new MapperImpl(), null);
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
}
