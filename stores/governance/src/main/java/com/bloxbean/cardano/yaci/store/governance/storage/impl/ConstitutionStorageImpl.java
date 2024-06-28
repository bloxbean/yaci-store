package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.Constitution;
import com.bloxbean.cardano.yaci.store.governance.storage.ConstitutionStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.ConstitutionMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.ConstitutionRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConstitutionStorageImpl implements ConstitutionStorage {
    private final ConstitutionRepository constitutionRepository;
    private final ConstitutionMapper constitutionMapper;

    @Override
    public void save(Constitution constitution) {
        constitutionRepository.save(constitutionMapper.toConstitutionEntity(constitution));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return constitutionRepository.deleteBySlotGreaterThan(slot);
    }
}
