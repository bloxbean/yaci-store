package com.bloxbean.cardano.yaci.store.staking.storage.impl;

import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper.StakingMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.DelegationEntityJpa;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.StakeRegistrationEntityJpa;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.DelegationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.StakeRegistrationRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StakeStorageImpl implements StakingStorage {
    private final StakeRegistrationRepository registrationRepository;
    private final DelegationRepository delegationRepository;
    private final StakingMapper mapper;

    @Override
    public void saveRegistrations(List<StakeRegistrationDetail> stakeRegistration) {
        List<StakeRegistrationEntityJpa> stakeRegistrationEntities = stakeRegistration.stream()
                        .map(stakeRegistrationDetail -> mapper.toStakeResistrationEntity(stakeRegistrationDetail))
                                .collect(Collectors.toList());
        registrationRepository.saveAll(stakeRegistrationEntities);
    }

    @Override
    public void saveDelegations(List<Delegation> delegations) {
        List<DelegationEntityJpa> delegationEntities = delegations.stream()
                .map(delegation -> mapper.toDelegationEntity(delegation))
                .collect(Collectors.toList());
        delegationRepository.saveAll(delegationEntities);
    }

    @Override
    public int deleteRegistrationsBySlotGreaterThan(Long slot) {
        return registrationRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public int deleteDelegationsBySlotGreaterThan(Long slot) {
        return delegationRepository.deleteBySlotGreaterThan(slot);
    }
}
