package com.bloxbean.cardano.yaci.store.api.staking.service;

import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StakeService {
    private final StakingStorageReader stakingStorageReader;

    public List<StakeRegistrationDetail> getStakeRegistrations(int p, int count) {
        return stakingStorageReader.findRegistrations(p, count);
    }

    public List<StakeRegistrationDetail> getStakeDeregistrations(int p, int count) {
        return stakingStorageReader.findDeregistrations(p, count);
    }

    public List<Delegation> getStakeDelegations(int p, int count) {
        return stakingStorageReader.findDelegations(p, count);
    }
}
