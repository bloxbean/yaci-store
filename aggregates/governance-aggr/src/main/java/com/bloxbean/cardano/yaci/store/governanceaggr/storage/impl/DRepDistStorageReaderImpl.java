package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepDist;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepDistStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.DRepDistMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.DRepDistRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class DRepDistStorageReaderImpl implements DRepDistStorageReader {
    private final DRepDistRepository dRepDistRepository;
    private final DRepDistMapper dRepDistMapper;

    @Override
    public Optional<BigInteger> getTotalStakeForEpoch(Integer epoch) {
        return dRepDistRepository.getTotalStakeForEpoch(epoch);
    }

    @Override
    public List<DRepDist> getAllByEpochAndDReps(Integer epoch, List<String> dRepIds) {
        return dRepDistRepository.getAllByEpochAndDReps(epoch, dRepIds)
                .stream()
                .map(dRepDistMapper::toDRepDist)
                .toList();
    }

    @Override
    public Optional<BigInteger> getStakeByDRepAndEpoch(String dRepId, Integer epoch) {
        return dRepDistRepository.getStakeByDRepAndEpoch(epoch, dRepId);
    }
}
