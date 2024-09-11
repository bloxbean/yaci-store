package com.bloxbean.cardano.yaci.store.governance.storage.local.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalDRepDistr;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalDRepDistrStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.LocalDRepDistrMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.LocalDRepDistrRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class LocalDRepDistrStorageReaderImpl implements LocalDRepDistrStorageReader {
    private final LocalDRepDistrRepository localDRepDistrRepository;
    private final LocalDRepDistrMapper localDRepDistrMapper;

    @Override
    public Optional<LocalDRepDistr> findLocalDRepDistrByDRepHashAndEpoch(String dRepHash, Integer epoch) {
        return localDRepDistrRepository.findByDrepHashAndEpoch(dRepHash, epoch)
                .map(localDRepDistrMapper::toLocalDRepDist);
    }
}
