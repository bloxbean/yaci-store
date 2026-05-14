package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.adapot.domain.WithdrawableReward;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorageReader;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdaPotStakeAccountRewardProviderTest {
    private static final String STAKE_ADDRESS = "stake_test1up97ct2wt8jqlly2cnkhuwc7tvevmjpp7h6ts3rucpksy8c8cnspn";

    @Test
    void getAccountInfoReturnsEmptyWhenRewardCalculationHasNotCompleted() {
        var rewardStorageReader = mock(RewardStorageReader.class);
        var adaPotJobStorage = mock(AdaPotJobStorage.class);
        when(adaPotJobStorage.getLatestJobByTypeAndStatus(AdaPotJobType.REWARD_CALC, AdaPotJobStatus.COMPLETED))
                .thenReturn(Optional.empty());
        var provider = new AdaPotStakeAccountRewardProvider(rewardStorageReader, adaPotJobStorage);

        var accountInfo = provider.getAccountInfo(STAKE_ADDRESS);

        assertThat(accountInfo).isEmpty();
        verify(rewardStorageReader, never()).findWithdrawableRewardByAddress(STAKE_ADDRESS);
    }

    @Test
    void getAccountInfoReturnsWithdrawableRewardWhenRewardCalculationHasCompleted() {
        var rewardStorageReader = mock(RewardStorageReader.class);
        var adaPotJobStorage = mock(AdaPotJobStorage.class);
        when(adaPotJobStorage.getLatestJobByTypeAndStatus(AdaPotJobType.REWARD_CALC, AdaPotJobStatus.COMPLETED))
                .thenReturn(Optional.of(AdaPotJob.builder()
                        .epoch(10)
                        .type(AdaPotJobType.REWARD_CALC)
                        .status(AdaPotJobStatus.COMPLETED)
                        .build()));
        when(rewardStorageReader.findWithdrawableRewardByAddress(STAKE_ADDRESS))
                .thenReturn(WithdrawableReward.builder()
                        .address(STAKE_ADDRESS)
                        .withdrawableAmount(BigInteger.valueOf(40))
                        .build());
        var provider = new AdaPotStakeAccountRewardProvider(rewardStorageReader, adaPotJobStorage);

        var accountInfo = provider.getAccountInfo(STAKE_ADDRESS);

        assertThat(accountInfo).isPresent();
        assertThat(accountInfo.orElseThrow().getStakeAddress()).isEqualTo(STAKE_ADDRESS);
        assertThat(accountInfo.orElseThrow().getWithdrawableAmount()).isEqualTo(BigInteger.valueOf(40));
    }
}
