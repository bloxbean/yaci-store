package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.EpochStakeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class EpochStakeStorageImpl implements EpochStakeStorage {
    private final EpochStakeRepository stakeSnapshotRepository;
    private final Mapper mapper;

    @Override
    public Optional<BigInteger> getTotalActiveStakeByEpoch(Integer activeEpoch) {
        return stakeSnapshotRepository.getTotalActiveStakeForEpoch(activeEpoch);
    }

    @Override
    public Optional<EpochStake> getActiveStakeByAddressAndEpoch(String address, Integer activeEpoch) {
        return stakeSnapshotRepository.findByAddressAndActiveEpoch(address, activeEpoch)
                .map(mapper::toEpochStake);
    }

    @Override
    public Optional<BigInteger> getActiveStakeByPoolAndEpoch(String poolId, Integer epoch) {
        return stakeSnapshotRepository.getActiveStakeByPoolAndEpoch(epoch, poolId);
    }

    @Override
    public List<EpochStake> getAllActiveStakesByEpoch(Integer epoch, int page, int count) {
        Pageable pageable = PageRequest.of(page, count);

        return stakeSnapshotRepository.getAllActiveStakesByEpoch(epoch, pageable)
                .stream().map(mapper::toEpochStake).toList();
    }
}
