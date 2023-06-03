package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.mapper.StakingMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.DelegationEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model.StakeRegistrationEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository.DelegationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository.StakeRegistrationRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StakeRegistrationStorageImpl implements StakingStorage {
    private final StakeRegistrationRepository registrationRepository;
    private final DelegationRepository delegationRepository;
    private final StakingMapper mapper;

    @Override
    public void saveRegistrations(List<StakeRegistrationDetail> stakeRegistration) {
        List<StakeRegistrationEntity> stakeRegistrationEntities = stakeRegistration.stream()
                        .map(stakeRegistrationDetail -> mapper.toStakeResistrationEntity(stakeRegistrationDetail))
                                .collect(Collectors.toList());
        registrationRepository.saveAll(stakeRegistrationEntities);
    }

    @Override
    public void saveDelegations(List<Delegation> delegations) {
        List<DelegationEntity> delegationEntities = delegations.stream()
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
