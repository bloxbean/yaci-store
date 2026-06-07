package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.core.model.PoolParams;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.domain.UnclaimedRewardRest;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.events.GenesisStaking;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import org.cardanofoundation.rewards.calculation.domain.PoolBlock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EpochRewardCalculationServiceTest {

    @Mock
    private TransactionStorageReader transactionStorageReader;

    @Mock
    private RewardStorage rewardStorage;

    @Mock
    private GenesisConfig genesisConfig;

    @Mock
    private BlockInfoService blockInfoService;

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

    @Test
    void includeDirectStartGenesisSlot0Block_addsOneBlockToInferredPool() {
        when(genesisConfig.getGenesisStaking()).thenReturn(new GenesisStaking(
                List.of(PoolParams.builder().operator("pool1").build()),
                List.of()));
        when(blockInfoService.hasPoolBlockAtSlot(0)).thenReturn(false);

        List<PoolBlock> adjustedBlocks = epochRewardCalculationService.includeDirectStartGenesisSlot0Block(
                0,
                0,
                List.of(PoolBlock.builder().poolId("pool1").blockCount(58).build()));

        assertThat(adjustedBlocks)
                .extracting(PoolBlock::getPoolId, PoolBlock::getBlockCount)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("pool1", 59));
    }

    @Test
    void includeDirectStartGenesisSlot0Block_doesNotChangePublicOrNonGenesisEpochs() {
        List<PoolBlock> poolBlocks = List.of(PoolBlock.builder().poolId("pool1").blockCount(58).build());

        assertThat(epochRewardCalculationService.includeDirectStartGenesisSlot0Block(0, 1, poolBlocks))
                .isSameAs(poolBlocks);
        assertThat(epochRewardCalculationService.includeDirectStartGenesisSlot0Block(1, 0, poolBlocks))
                .isSameAs(poolBlocks);

        verify(genesisConfig, never()).getGenesisStaking();
    }

    @Test
    void includeDirectStartGenesisSlot0Block_doesNotDoubleCountExistingSlot0Block() {
        when(genesisConfig.getGenesisStaking()).thenReturn(new GenesisStaking(
                List.of(PoolParams.builder().operator("pool1").build()),
                List.of()));
        when(blockInfoService.hasPoolBlockAtSlot(0)).thenReturn(true);

        List<PoolBlock> poolBlocks = List.of(PoolBlock.builder().poolId("pool1").blockCount(59).build());

        assertThat(epochRewardCalculationService.includeDirectStartGenesisSlot0Block(0, 0, poolBlocks))
                .isSameAs(poolBlocks);
    }

}
