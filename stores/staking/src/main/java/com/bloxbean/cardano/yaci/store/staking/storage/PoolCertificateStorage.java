package com.bloxbean.cardano.yaci.store.staking.storage;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;

import java.util.List;

public interface PoolCertificateStorage {
    void savePoolRegistrations(List<PoolRegistration> poolRegistrations);
    void savePoolRetirements(List<PoolRetirement> poolRetirements);

    int deleteRegistrationsBySlotGreaterThan(Long slot);
    int deleteRetirementsBySlotGreaterThan(Long slot);
}
