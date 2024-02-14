package com.bloxbean.cardano.yaci.store.staking.storage;

import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;

import java.util.List;

public interface StakingCertificteStorageReader {
    List<StakeRegistrationDetail> findRegistrations(int page, int count);
    List<StakeRegistrationDetail> findDeregistrations(int page, int count);

    List<Delegation> findDelegations(int page, int count);
}