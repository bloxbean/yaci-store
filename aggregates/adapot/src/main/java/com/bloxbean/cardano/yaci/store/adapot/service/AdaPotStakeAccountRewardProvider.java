package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAccountRewardInfo;
import com.bloxbean.cardano.yaci.store.account.service.StakeAccountRewardProvider;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorageReader;
import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdaPotStakeAccountRewardProvider implements StakeAccountRewardProvider {
    private final RewardStorageReader rewardStorageReader;
    private final AdaPotJobStorage adaPotJobStorage;
    private final StakingCertificateStorageReader stakingCertificateStorageReader;

    @Override
    public Optional<StakeAccountRewardInfo> getAccountInfo(String stakeAddress) {
        var latestCompletedRewardCalc = adaPotJobStorage.getLatestJobByTypeAndStatus(AdaPotJobType.REWARD_CALC, AdaPotJobStatus.COMPLETED);
        if (latestCompletedRewardCalc.isEmpty()) {
            return Optional.empty();
        }

        var withdrawableReward = rewardStorageReader.findWithdrawableRewardByAddress(stakeAddress);

        var latestRegistration = stakingCertificateStorageReader.getRegistrationByStakeAddress(stakeAddress, Long.MAX_VALUE);
        boolean isDeregistered = latestRegistration
                .map(reg -> reg.getType() == CertificateType.STAKE_DEREGISTRATION)
                .orElse(false);

        String poolId = null;
        if (!isDeregistered) {
            poolId = stakingCertificateStorageReader.getLatestDelegationByAddress(stakeAddress)
                    .map(delegation -> PoolUtil.getBech32PoolId(delegation.getPoolId()))
                    .orElse(null);
        }

        return Optional.of(StakeAccountRewardInfo.builder()
                .stakeAddress(stakeAddress)
                .withdrawableAmount(withdrawableReward.getWithdrawableAmount())
                .poolId(poolId)
                .build());
    }
}
