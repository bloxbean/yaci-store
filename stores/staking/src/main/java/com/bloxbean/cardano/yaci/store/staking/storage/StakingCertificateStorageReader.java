package com.bloxbean.cardano.yaci.store.staking.storage;

import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;

import java.util.List;
import java.util.Optional;

public interface StakingCertificateStorageReader {
    List<StakeRegistrationDetail> findRegistrations(int page, int count);
    List<StakeRegistrationDetail> findDeregistrations(int page, int count);

    List<Delegation> findDelegations(int page, int count);

    List<String> getRegisteredStakeAddresses(Integer epoch, int page, int count);

    Optional<StakeRegistrationDetail> getRegistrationByStakeAddress(String stakeAddress, Long slot);

    Optional<StakeRegistrationDetail> getRegistrationByPointer(long slot, int txIndex, int certIndex);
}
