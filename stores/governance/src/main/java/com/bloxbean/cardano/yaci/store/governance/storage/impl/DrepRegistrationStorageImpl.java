package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.DrepRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.DrepRegistrationStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.DrepRegistrationMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.DrepRegistrationRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DrepRegistrationStorageImpl implements DrepRegistrationStorage {

    private final DrepRegistrationRepository drepRegistrationRepository;
    private final DrepRegistrationMapper drepRegistrationMapper;

    @Override
    public void saveAll(List<DrepRegistration> drepRegistrations) {
        drepRegistrationRepository.saveAll(drepRegistrations.stream()
                .map(drepRegistrationMapper::toDrepRegistrationEntity)
                .collect(Collectors.toList()));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return drepRegistrationRepository.deleteBySlotGreaterThan(slot);
    }
}
