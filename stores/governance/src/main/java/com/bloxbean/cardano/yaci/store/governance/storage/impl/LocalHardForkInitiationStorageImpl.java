package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalHardForkInitiation;
import com.bloxbean.cardano.yaci.store.governance.storage.LocalHardForkInitiationStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.LocalHardForkInitiationMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.LocalHardForkInitiationRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class LocalHardForkInitiationStorageImpl implements LocalHardForkInitiationStorage {
    private final LocalHardForkInitiationRepository localHardForkInitiationRepository;
    private final LocalHardForkInitiationMapper localHardForkInitiationMapper;

    @Override
    public void saveAll(List<LocalHardForkInitiation> localHardForkInitiationList) {
        localHardForkInitiationRepository.saveAll(localHardForkInitiationList.stream()
                .map(localHardForkInitiationMapper::toLocalHardForkInitiationEntity)
                .toList());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return localHardForkInitiationRepository.deleteBySlotGreaterThan(slot);
    }
}
