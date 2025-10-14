package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.DRepRegistrationStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.DRepRegistrationMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.DRepRegistrationRepository;
import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DRepRegistrationStorageImpl implements DRepRegistrationStorage {

    private static final String PLUGIN_DREP_REGISTRATION_SAVE = "governance.drep_registration.save";
    private final DRepRegistrationRepository drepRegistrationRepository;
    private final DRepRegistrationMapper drepRegistrationMapper;

    @Override
    @Plugin(key = PLUGIN_DREP_REGISTRATION_SAVE)
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
