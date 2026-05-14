package com.bloxbean.cardano.yaci.store.account.service;

import com.bloxbean.cardano.yaci.store.account.domain.StakeAccountRewardInfo;

import java.util.Optional;

public interface StakeAccountRewardProvider {
    Optional<StakeAccountRewardInfo> getAccountInfo(String stakeAddress);
}
