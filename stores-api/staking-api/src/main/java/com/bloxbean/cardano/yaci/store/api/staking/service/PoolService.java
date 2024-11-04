package com.bloxbean.cardano.yaci.store.api.staking.service;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolDetails;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolCertificateStorageReader;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PoolService {
    private final PoolCertificateStorageReader poolCertStorageReader;
    private final PoolStorageReader poolStorageReader;

    public List<PoolRegistration> getPoolRegistrations(int page, int count) {
        return poolCertStorageReader.findPoolRegistrations(page, count);
    }

    public List<PoolRetirement> getPoolRetirements(int page, int count) {
        return poolCertStorageReader.findPoolRetirements(page, count);
    }

    public List<PoolRetirement> getRetiringPoolIds(int epoch) {
        return poolCertStorageReader.getRetiringPools(epoch);
    }

    public Optional<PoolDetails> getPoolDetails(String poolId, int epoch) {
        return poolStorageReader.getPoolDetails(List.of(poolId), epoch)
                .stream()
                .findFirst();
    }

}
