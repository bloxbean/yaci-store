package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepPage;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.DRepRegistrationStorageReader;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.DRepRegistrationMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepRegistrationEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.DRepRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DRepRegistrationStorageReaderImpl implements DRepRegistrationStorageReader {
    private final DRepRegistrationRepository drepRegistrationRepository;
    private final DRepRegistrationMapper drepRegistrationMapper;

    @Override
    public List<DRepRegistration> findRegistrations(int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        return drepRegistrationRepository.findRegistrations(sortedBySlot)
                .stream()
                .map(drepRegistrationMapper::toDRepRegistration)
                .collect(Collectors.toList());
    }

    @Override
    public List<DRepRegistration> findDeRegistrations(int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        return drepRegistrationRepository.findDeRegistrations(sortedBySlot)
                .stream()
                .map(drepRegistrationMapper::toDRepRegistration)
                .collect(Collectors.toList());
    }

    @Override
    public List<DRepRegistration> findUpdates(int page, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());

        return drepRegistrationRepository.findUpdates(sortedBySlot)
                .stream()
                .map(drepRegistrationMapper::toDRepRegistration)
                .collect(Collectors.toList());
    }

    @Override
    public DRepPage findDReps(int p, int count, Order order) {
        Pageable sortedBySlot =
                PageRequest.of(p, count, order == Order.asc ? Sort.by("slot").ascending() : Sort.by("slot").descending());
        Page<DRepRegistrationEntity> all= drepRegistrationRepository.findAllDreps(sortedBySlot);
        return DRepPage.builder()
                .total(all.getTotalElements())
                .totalPages(all.getTotalPages())
                .dReps(all.map(drepRegistrationMapper::toDRepRegistration).stream().toList())
                .build();
    }
}
