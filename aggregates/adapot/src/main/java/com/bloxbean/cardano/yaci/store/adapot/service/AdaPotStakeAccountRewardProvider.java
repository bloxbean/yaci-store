package com.bloxbean.cardano.yaci.store.adapot.service;

import com.bloxbean.cardano.yaci.store.account.domain.StakeAccountRewardInfo;
import com.bloxbean.cardano.yaci.store.account.service.StakeAccountRewardProvider;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdaPotStakeAccountRewardProvider implements StakeAccountRewardProvider {
    private final RewardStorageReader rewardStorageReader;
    private final AdaPotJobStorage adaPotJobStorage;

    @Override
    public Optional<StakeAccountRewardInfo> getAccountInfo(String stakeAddress) {
        var latestCompletedRewardCalc = adaPotJobStorage.getLatestJobByTypeAndStatus(AdaPotJobType.REWARD_CALC, AdaPotJobStatus.COMPLETED);
        if (latestCompletedRewardCalc.isEmpty()) {
            return Optional.empty();
        }

        var withdrawableReward = rewardStorageReader.findWithdrawableRewardByAddress(stakeAddress, latestCompletedRewardCalc.get().getEpoch());
        return Optional.of(StakeAccountRewardInfo.builder()
                .stakeAddress(stakeAddress)
                .withdrawableAmount(withdrawableReward.getWithdrawableAmount())
                .build());
    }
}
