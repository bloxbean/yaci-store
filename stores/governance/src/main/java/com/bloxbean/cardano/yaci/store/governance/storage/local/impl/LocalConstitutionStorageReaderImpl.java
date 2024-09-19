package com.bloxbean.cardano.yaci.store.governance.storage.local.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalConstitution;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalConstitutionStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.LocalConstitutionMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.LocalConstitutionRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class LocalConstitutionStorageReaderImpl implements LocalConstitutionStorageReader {
    private final LocalConstitutionRepository localConstitutionRepository;
    private final LocalConstitutionMapper localConstitutionMapper;
    
    @Override
    public Optional<LocalConstitution> findByMaxSlot() {
        return localConstitutionRepository.findFirstByOrderBySlotDesc()
                .map(localConstitutionMapper::toLocalConstitution);
    }
}
