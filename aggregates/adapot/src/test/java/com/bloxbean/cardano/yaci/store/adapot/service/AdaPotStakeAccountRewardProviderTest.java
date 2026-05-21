package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.store.adapot.domain.WithdrawableReward;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorageReader;
import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorageReader;
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
    private static final String POOL_HASH = "0f292d1679c3417e1e0e60a810a3e4f3e4e8e8e8e8e8e8e8e8e8e8e8e8e8e8e8";

    @Test
    void getAccountInfoReturnsEmptyWhenRewardCalculationHasNotCompleted() {
        var rewardStorageReader = mock(RewardStorageReader.class);
        var adaPotJobStorage = mock(AdaPotJobStorage.class);
        var stakingCertificateStorageReader = mock(StakingCertificateStorageReader.class);
        when(adaPotJobStorage.getLatestJobByTypeAndStatus(AdaPotJobType.REWARD_CALC, AdaPotJobStatus.COMPLETED))
                .thenReturn(Optional.empty());
        var provider = new AdaPotStakeAccountRewardProvider(rewardStorageReader, adaPotJobStorage, stakingCertificateStorageReader);

        var accountInfo = provider.getAccountInfo(STAKE_ADDRESS);

        assertThat(accountInfo).isEmpty();
        verify(rewardStorageReader, never()).findWithdrawableRewardByAddress(STAKE_ADDRESS);
    }

    @Test
    void getAccountInfoReturnsWithdrawableRewardAndPoolIdWhenDelegationExists() {
        var rewardStorageReader = mock(RewardStorageReader.class);
        var adaPotJobStorage = mock(AdaPotJobStorage.class);
        var stakingCertificateStorageReader = mock(StakingCertificateStorageReader.class);
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
        when(stakingCertificateStorageReader.getRegistrationByStakeAddress(STAKE_ADDRESS, Long.MAX_VALUE))
                .thenReturn(Optional.of(StakeRegistrationDetail.builder()
                        .address(STAKE_ADDRESS)
                        .type(CertificateType.STAKE_REGISTRATION)
                        .build()));
        when(stakingCertificateStorageReader.getLatestDelegationByAddress(STAKE_ADDRESS))
                .thenReturn(Optional.of(Delegation.builder()
                        .address(STAKE_ADDRESS)
                        .poolId(POOL_HASH)
                        .build()));
        var provider = new AdaPotStakeAccountRewardProvider(rewardStorageReader, adaPotJobStorage, stakingCertificateStorageReader);

        var accountInfo = provider.getAccountInfo(STAKE_ADDRESS);

        assertThat(accountInfo).isPresent();
        assertThat(accountInfo.orElseThrow().getStakeAddress()).isEqualTo(STAKE_ADDRESS);
        assertThat(accountInfo.orElseThrow().getWithdrawableAmount()).isEqualTo(BigInteger.valueOf(40));
        assertThat(accountInfo.orElseThrow().getPoolId()).startsWith("pool");
    }

    @Test
    void getAccountInfoReturnsNullPoolIdWhenNoDelegationExists() {
        var rewardStorageReader = mock(RewardStorageReader.class);
        var adaPotJobStorage = mock(AdaPotJobStorage.class);
        var stakingCertificateStorageReader = mock(StakingCertificateStorageReader.class);
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
        when(stakingCertificateStorageReader.getRegistrationByStakeAddress(STAKE_ADDRESS, Long.MAX_VALUE))
                .thenReturn(Optional.of(StakeRegistrationDetail.builder()
                        .address(STAKE_ADDRESS)
                        .type(CertificateType.STAKE_REGISTRATION)
                        .build()));
        when(stakingCertificateStorageReader.getLatestDelegationByAddress(STAKE_ADDRESS))
                .thenReturn(Optional.empty());
        var provider = new AdaPotStakeAccountRewardProvider(rewardStorageReader, adaPotJobStorage, stakingCertificateStorageReader);

        var accountInfo = provider.getAccountInfo(STAKE_ADDRESS);

        assertThat(accountInfo).isPresent();
        assertThat(accountInfo.orElseThrow().getStakeAddress()).isEqualTo(STAKE_ADDRESS);
        assertThat(accountInfo.orElseThrow().getWithdrawableAmount()).isEqualTo(BigInteger.valueOf(40));
        assertThat(accountInfo.orElseThrow().getPoolId()).isNull();
    }

    @Test
    void getAccountInfoReturnsNullPoolIdWhenStakeKeyIsDeregistered() {
        var rewardStorageReader = mock(RewardStorageReader.class);
        var adaPotJobStorage = mock(AdaPotJobStorage.class);
        var stakingCertificateStorageReader = mock(StakingCertificateStorageReader.class);
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
        when(stakingCertificateStorageReader.getRegistrationByStakeAddress(STAKE_ADDRESS, Long.MAX_VALUE))
                .thenReturn(Optional.of(StakeRegistrationDetail.builder()
                        .address(STAKE_ADDRESS)
                        .type(CertificateType.STAKE_DEREGISTRATION)
                        .build()));
        var provider = new AdaPotStakeAccountRewardProvider(rewardStorageReader, adaPotJobStorage, stakingCertificateStorageReader);

        var accountInfo = provider.getAccountInfo(STAKE_ADDRESS);

        assertThat(accountInfo).isPresent();
        assertThat(accountInfo.orElseThrow().getWithdrawableAmount()).isEqualTo(BigInteger.valueOf(40));
        assertThat(accountInfo.orElseThrow().getPoolId()).isNull();
        verify(stakingCertificateStorageReader, never()).getLatestDelegationByAddress(STAKE_ADDRESS);
    }
}
