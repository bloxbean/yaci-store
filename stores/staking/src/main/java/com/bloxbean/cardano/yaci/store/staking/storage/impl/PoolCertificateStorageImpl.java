package com.bloxbean.cardano.yaci.store.staking.storage.impl;

import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolCertificateStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper.PoolMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolRegistrationEnity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolRetirementEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.PoolRegistrationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.PoolRetirementRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PoolCertificateStorageImpl implements PoolCertificateStorage {
    private final static String PLUGIN_POOL_REGISTRATION_SAVE = "staking.pool_registration.save";
    private final static String PLUGIN_POOL_RETIREMENT_SAVE = "staking.pool_retirement.save";

    private final PoolRegistrationRepository poolRegistrationRepository;
    private final PoolRetirementRepository poolRetirementRepository;
    private final PoolMapper mapper;

    @Override
    @Plugin(key = PLUGIN_POOL_REGISTRATION_SAVE)
    public void savePoolRegistrations(List<PoolRegistration> poolRegistrations) {
        List<PoolRegistrationEnity> poolRegistrationEnities = poolRegistrations.stream()
                .map(mapper::toPoolRegistrationEntity)
                .collect(Collectors.toList());

        poolRegistrationRepository.saveAll(poolRegistrationEnities);
    }

    @Override
    @Plugin(key = PLUGIN_POOL_RETIREMENT_SAVE)
    public void savePoolRetirements(List<PoolRetirement> poolRetirements) {
        List<PoolRetirementEntity> poolRetirementEntities = poolRetirements.stream()
                .map(mapper::toPoolRetirementEntity)
                .collect(Collectors.toList());

        poolRetirementRepository.saveAll(poolRetirementEntities);
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
