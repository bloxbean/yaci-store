package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeRegistrationStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.CommitteeRegistrationMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.CommitteeRegistrationRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommitteeRegistrationStorageImpl implements CommitteeRegistrationStorage {

    private final CommitteeRegistrationRepository committeeRegistrationRepository;
    private final CommitteeRegistrationMapper committeeRegistrationMapper;

    @Override
    public void saveAll(List<CommitteeRegistration> committeeRegistrations) {
        committeeRegistrationRepository.saveAll(committeeRegistrations.stream()
                .map(committeeRegistrationMapper::toCommitteeRegistrationEntity)
                .collect(Collectors.toList()));
    }


    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return committeeRegistrationRepository.deleteBySlotGreaterThan(slot);
    }
}
