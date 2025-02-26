package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.domain.UnclaimedRewardRest;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EpochRewardCalculationServiceTest {

    @Mock
    private TransactionStorageReader transactionStorageReader;

    @Mock
    private RewardStorage rewardStorage;

    @InjectMocks
    private EpochRewardCalculationService epochRewardCalculationService;

    @Test
    void adjustTreasuryAmount() {
        int epoch = 5;

        when(transactionStorageReader.getTotalDonation(epoch - 1)).thenReturn(adaToLovelace(5));
        when(rewardStorage.findUnclaimedRewardRest(epoch)).thenReturn(List.of(UnclaimedRewardRest.builder()
                .spendableEpoch(epoch)
                .earnedEpoch(epoch - 1)
                .type(RewardRestType.proposal_refund)
                .amount(adaToLovelace(100))
                .address("addr_test1qrlvt2gzuvrhq7m2k00rsyzfrrqwx085cdqgum7w5nc2rxwpxkp2ajdyflxxmxztuqpu2pvvvc8p6tl3xu8a3dym5uls50mr97\n")
                .build()
        ));
        when(rewardStorage.findTreasuryWithdrawals(epoch)).thenReturn(List.of(
                RewardRest.builder()
                        .address("stake_test1urqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y4p")
                        .earnedEpoch(epoch)
                        .spendableEpoch(epoch)
                        .type(RewardRestType.treasury)
                        .amount(adaToLovelace(200))
                        .build(),
                RewardRest.builder()
                        .address("stake_test1vqqntq4wexjylnrdnp97qq79qkxxvrsa9lcnwr7ckjd6w0cr04y5p")
                        .earnedEpoch(epoch)
                        .spendableEpoch(epoch)
                        .type(RewardRestType.treasury)
                        .amount(adaToLovelace(500))
                        .build()
        ));

        var newTreasuryAmount = epochRewardCalculationService.adjustTreasuryAmount(epoch, adaToLovelace(100000));

        var expectedTreasury = adaToLovelace(100000)
                .add(adaToLovelace(5)) //donation
                .add(adaToLovelace(100)) //unclaimed rewards
                .subtract(adaToLovelace(200 + 500)); //treasury withdrawals

        assertThat(newTreasuryAmount).isEqualTo(expectedTreasury);
    }


}
