package com.bloxbean.cardano.yaci.store.staking.service;

import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StakeService {
    private final StakingStorage stakingStorage;

    public List<StakeRegistrationDetail> getStakeRegistrations(int p, int count) {
        return stakingStorage.findRegistrations(p, count);
    }

    public List<StakeRegistrationDetail> getStakeDeregistrations(int p, int count) {
        return stakingStorage.findDeregistrations(p, count);
    }

    public List<Delegation> getStakeDelegations(int p, int count) {
        return stakingStorage.findDelegations(p, count);
    }
}
