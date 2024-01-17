package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.DRepRegistrationStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.DRepRegistrationMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.DRepRegistrationRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DRepRegistrationStorageImpl implements DRepRegistrationStorage {

    private final DRepRegistrationRepository drepRegistrationRepository;
    private final DRepRegistrationMapper drepRegistrationMapper;

    @Override
    public void saveAll(List<DRepRegistration> dRepRegistrations) {
        drepRegistrationRepository.saveAll(dRepRegistrations.stream()
                .map(drepRegistrationMapper::toDRepRegistrationEntity)
                .collect(Collectors.toList()));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return drepRegistrationRepository.deleteBySlotGreaterThan(slot);
    }
}
