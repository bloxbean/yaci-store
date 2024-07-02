package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.Constitution;
import com.bloxbean.cardano.yaci.store.governance.storage.ConstitutionStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.ConstitutionMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.ConstitutionRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class ConstitutionStorageReaderImpl implements ConstitutionStorageReader {
    private final ConstitutionRepository constitutionRepository;
    private final ConstitutionMapper constitutionMapper;

    @Override
    public Optional<Constitution> findCurrentConstitution() {
        return constitutionRepository.findFirstByOrderBySlotDesc()
                .map(constitutionMapper::toConstitution);
    }
}
