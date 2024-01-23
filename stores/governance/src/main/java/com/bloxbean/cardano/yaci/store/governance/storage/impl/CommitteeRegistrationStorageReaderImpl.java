package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeRegistrationStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.CommitteeRegistrationMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.CommitteeRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommitteeRegistrationStorageReaderImpl implements CommitteeRegistrationStorageReader {
    private final CommitteeRegistrationRepository committeeRegistrationRepository;
    private final CommitteeRegistrationMapper committeeRegistrationMapper;

    @Override
    public List<CommitteeRegistration> findAll(int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return committeeRegistrationRepository.findAll(sortedBySlot)
                .stream()
                .map(committeeRegistrationMapper::toCommitteeRegistration)
                .collect(Collectors.toList());
    }
}
