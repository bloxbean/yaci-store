package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeDeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeDeRegistrationStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.CommitteeDeRegistrationMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.CommitteeDeRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommitteeDeRegistrationStorageReaderImpl implements CommitteeDeRegistrationStorageReader {
    private final CommitteeDeRegistrationRepository committeeDeRegistrationRepository;
    private final CommitteeDeRegistrationMapper committeeDeRegistrationMapper;

    @Override
    public List<CommitteeDeRegistration> findAll(int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return committeeDeRegistrationRepository.findAll(sortedBySlot)
                .stream()
                .map(committeeDeRegistrationMapper::toCommitteeDeRegistration)
                .collect(Collectors.toList());
    }
}
