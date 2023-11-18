package com.bloxbean.cardano.yaci.store.staking.storage.impl;

import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingStorageReader;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper.StakingMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.DelegationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.StakeRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class StakeStorageReaderImpl implements StakingStorageReader {
    private final StakeRegistrationRepository registrationRepository;
    private final DelegationRepository delegationRepository;
    private final StakingMapper mapper;

    @Override
    public List<StakeRegistrationDetail> findRegistrations(int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return registrationRepository.findRegistrations(sortedBySlot)
                .stream()
                .map(stakeRegistrationEntity -> mapper.toStakeRegistrationDetail(stakeRegistrationEntity))
                .collect(Collectors.toList());
    }

    @Override
    public List<StakeRegistrationDetail> findDeregistrations(int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return registrationRepository.findDeregestrations(sortedBySlot)
                .stream()
                .map(stakeRegistrationEntity -> mapper.toStakeRegistrationDetail(stakeRegistrationEntity))
                .collect(Collectors.toList());
    }

    @Override
    public List<Delegation> findDelegations(int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return delegationRepository.findDelegations(sortedBySlot)
                .stream()
                .map(delegationEntity -> mapper.toDelegation(delegationEntity))
                .collect(Collectors.toList());
    }

}
