package com.bloxbean.cardano.yaci.store.staking.storage.impl;

import com.bloxbean.cardano.yaci.store.plugin.aspect.Plugin;
import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper.StakingMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.DelegationEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.StakeRegistrationEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.DelegationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.StakeRegistrationRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StakeCertificateStorageImpl implements StakingCertificateStorage {
    private final static String FILTER_STAKE_KEYREGISTRATION_SAVE = "staking.key_registration.save";
    private final static String FILTER_DELEGATION_SAVE = "staking.key_delegation.save";

    private final StakeRegistrationRepository registrationRepository;
    private final DelegationRepository delegationRepository;
    private final StakingMapper mapper;

    @Override
    @Plugin(key = FILTER_STAKE_KEYREGISTRATION_SAVE)
    public void saveRegistrations(List<StakeRegistrationDetail> stakeRegistration) {
        List<StakeRegistrationEntity> stakeRegistrationEntities = stakeRegistration.stream()
                        .map(stakeRegistrationDetail -> mapper.toStakeResistrationEntity(stakeRegistrationDetail))
                                .collect(Collectors.toList());
        registrationRepository.saveAll(stakeRegistrationEntities);
    }

    @Override
    @Plugin(key = FILTER_DELEGATION_SAVE)
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
