package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.mapper.PoolMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.PoolRegistrationEnity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.PoolRetirementEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository.PoolRegistrationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository.PoolRetirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PoolStorageImpl implements PoolStorage {
    private final PoolRegistrationRepository poolRegistrationRepository;
    private final PoolRetirementRepository poolRetirementRepository;
    private final PoolMapper mapper;

    @Override
    public void savePoolRegistrations(List<PoolRegistration> poolRegistrations) {
        List<PoolRegistrationEnity> poolRegistrationEnities = poolRegistrations.stream()
                .map(mapper::toPoolRegistrationEntity)
                .collect(Collectors.toList());

        poolRegistrationRepository.saveAll(poolRegistrationEnities);
    }

    @Override
    public void savePoolRetirements(List<PoolRetirement> poolRetirements) {
        List<PoolRetirementEntity> poolRetirementEntities = poolRetirements.stream()
                .map(mapper::toPoolRetirementEntity)
                .collect(Collectors.toList());

        poolRetirementRepository.saveAll(poolRetirementEntities);
    }

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

    @Override
    public int deleteRegistrationsBySlotGreaterThan(Long slot) {
        return poolRegistrationRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public int deleteRetirementsBySlotGreaterThan(Long slot) {
        return poolRetirementRepository.deleteBySlotGreaterThan(slot);
    }
}
