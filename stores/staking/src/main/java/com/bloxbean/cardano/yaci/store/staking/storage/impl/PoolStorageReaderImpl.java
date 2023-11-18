package com.bloxbean.cardano.yaci.store.staking.storage.impl;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorageReader;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper.PoolMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.PoolRegistrationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.PoolRetirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PoolStorageReaderImpl implements PoolStorageReader {
    private final PoolRegistrationRepository poolRegistrationRepository;
    private final PoolRetirementRepository poolRetirementRepository;
    private final PoolMapper mapper;

    @Override
    public List<PoolRegistration> findPoolRegistrations(int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return poolRegistrationRepository.findAllPools(sortedBySlot)
                .stream()
                .map(mapper::toPoolRegistration)
                .collect(Collectors.toList());
    }

    @Override
    public List<PoolRetirement> findPoolRetirements(int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return poolRetirementRepository.findAllPools(sortedBySlot)
                .stream()
                .map(mapper::toPoolRetirement)
                .collect(Collectors.toList());
    }
}
