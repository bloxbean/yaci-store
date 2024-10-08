package com.bloxbean.cardano.yaci.store.governance.storage.local.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalConstitution;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalConstitutionStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.LocalConstitutionMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.LocalConstitutionRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalConstitutionStorageImpl implements LocalConstitutionStorage {
    private final LocalConstitutionRepository localConstitutionRepository;
    private final LocalConstitutionMapper localConstitutionMapper;

    @Override
    public void save(LocalConstitution localConstitution) {
        localConstitutionRepository.save(localConstitutionMapper.toLocalConstitutionEntity(localConstitution));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return localConstitutionRepository.deleteBySlotGreaterThan(slot);
    }
}
