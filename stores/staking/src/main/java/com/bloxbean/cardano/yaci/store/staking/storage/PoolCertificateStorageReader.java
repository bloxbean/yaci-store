package com.bloxbean.cardano.yaci.store.staking.storage;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;

import java.util.List;

public interface PoolCertificateStorageReader {
    List<PoolRegistration> findPoolRegistrations(int page, int count);
    List<PoolRetirement> findPoolRetirements(int page, int count);

    List<PoolRetirement> getRetiringPools(int epoch);

}
