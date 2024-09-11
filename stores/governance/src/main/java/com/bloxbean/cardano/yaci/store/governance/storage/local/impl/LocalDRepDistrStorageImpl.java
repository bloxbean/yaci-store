package com.bloxbean.cardano.yaci.store.governance.storage.local.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalDRepDistr;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalDRepDistrStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.LocalDRepDistrMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.LocalDRepDistrRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class LocalDRepDistrStorageImpl implements LocalDRepDistrStorage {
    private final LocalDRepDistrRepository localDRepDistrRepository;
    private final LocalDRepDistrMapper localDRepDistrMapper;

    @Override
    public void saveAll(List<LocalDRepDistr> localDRepDistrList) {
        localDRepDistrRepository.saveAll(localDRepDistrList.stream()
                .map(localDRepDistrMapper::localDRepDistrEntity).toList());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return localDRepDistrRepository.deleteBySlotGreaterThan(slot);
    }
}
