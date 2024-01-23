package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeDeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeDeRegistrationStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.CommitteeDeRegistrationMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.CommitteeDeRegistrationRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommitteeDeRegistrationStorageImpl implements CommitteeDeRegistrationStorage {
    private final CommitteeDeRegistrationRepository committeeDeRegistrationRepository;
    private final CommitteeDeRegistrationMapper committeeDeRegistrationMapper;

    @Override
    public void saveAll(List<CommitteeDeRegistration> committeeDeRegistrations) {
        committeeDeRegistrationRepository.saveAll(committeeDeRegistrations.stream()
                .map(committeeDeRegistrationMapper::toCommitteeDeRegistrationEntity)
                .collect(Collectors.toList()));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return committeeDeRegistrationRepository.deleteBySlotGreaterThan(slot);
    }
}
