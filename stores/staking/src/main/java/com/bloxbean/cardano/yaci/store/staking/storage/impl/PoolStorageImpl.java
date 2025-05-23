package com.bloxbean.cardano.yaci.store.staking.storage.impl;

import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import com.bloxbean.cardano.yaci.store.staking.domain.Pool;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolStatusType;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper.PoolMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.PoolStatusRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PoolStorageImpl implements PoolStorage {
    private final static String FILTER_POOL_SAVE = "staking.pool.save";

    private final PoolStatusRepository poolStatusRepository;
    private final PoolMapper poolMapper;

    @Override
    @Plugin(key = FILTER_POOL_SAVE)
    public void save(List<Pool> poolStatuses) {
        if (poolStatuses == null || poolStatuses.isEmpty())
            return;

        poolStatusRepository.saveAll(poolStatuses.stream()
                .map(poolMapper::toDepositEntity).collect(Collectors.toList()));
    }

    @Override
    public Optional<Pool> findRecentPoolRegistration(String poolId, Integer maxEpoch) {
        return poolStatusRepository.findRecentByPoolIdAndStatus(poolId, PoolStatusType.REGISTRATION, maxEpoch)
                .map(poolMapper::toDeposit);
    }

    @Override
    public Optional<Pool> findRecentPoolUpdate(String poolId, Integer maxEpoch) {
        return poolStatusRepository.findRecentByPoolIdAndStatus(poolId, PoolStatusType.UPDATE, maxEpoch)
                .map(poolMapper::toDeposit);
    }

    @Override
    public Optional<Pool> findRecentPoolRetirement(String poolId, Integer maxEpoch) {
        return poolStatusRepository.findRecentPoolRetirement(poolId, maxEpoch)
                .map(poolMapper::toDeposit);
    }

    @Override
    public Optional<Pool> findRecentPoolRetired(String poolId, Integer maxEpoch) {
        return poolStatusRepository.findRecentByPoolIdAndStatus(poolId, PoolStatusType.RETIRED, maxEpoch)
                .map(poolMapper::toDeposit);
    }

    @Override
    public List<Pool> findRetiringPools(Integer epoch) {
        return poolStatusRepository.findRetiringPoolsByRetireEpoch(epoch)
                .stream().map(poolMapper::toDeposit).collect(Collectors.toList());
    }

    @Override
    public List<Pool> findRetiredPools(Integer epoch) {
        return poolStatusRepository.findRetiredPoolsByRetireEpoch(epoch)
                .stream().map(poolMapper::toDeposit).collect(Collectors.toList());
    }

    @Override
    public List<Pool> findActivePools(Integer epoch) {
        return poolStatusRepository.findActivePoolsByEpoch(epoch)
                .stream().map(poolMapper::toDeposit).collect(Collectors.toList());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return poolStatusRepository.deleteBySlotGreaterThan(slot);
    }
}
