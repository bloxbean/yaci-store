package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.EpochStakeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class EpochStakeStorageReaderImpl implements EpochStakeStorageReader {
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

    @Override
    public List<EpochStake> getAllActiveStakesByEpochAndPool(Integer epoch, String poolId, int page, int count) {
        Pageable pageable = PageRequest.of(page, count);

        return stakeSnapshotRepository.getAllByActiveEpochAndPool(epoch, poolId, pageable)
                .stream().map(mapper::toEpochStake).toList();
    }

    @Override
    public List<EpochStake> getAllActiveStakesByEpochAndPool(Integer epoch, String poolId) {
        List<EpochStake> allActiveStakes = new ArrayList<>();
        int page = 0;
        int count = 1000;
        List<EpochStake> pageStakes;

        do {
            pageStakes = getAllActiveStakesByEpochAndPool(epoch, poolId, page, count);
            allActiveStakes.addAll(pageStakes);
            page++;
        } while (!pageStakes.isEmpty());

        return allActiveStakes;
    }

    @Override
    public List<EpochStake> getAllActiveStakesByEpochAndPools(Integer epoch, List<String> poolIds) {
        return stakeSnapshotRepository.getAllByActiveEpochAndPools(epoch, poolIds)
                .stream().map(mapper::toEpochStake).toList();
    }

}
