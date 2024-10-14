package com.bloxbean.cardano.yaci.store.staking.storage.impl;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolCertificateStorageReader;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper.PoolMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolRegistrationId;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.PoolRegistrationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.PoolRetirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PoolCertificateStorageReaderImpl implements PoolCertificateStorageReader {
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

    @Override
    public List<PoolRetirement> getRetiringPools(int epoch) {
        //Get all pool ids with retirement epoch == epoch
        var poolsWithRetirementEpoch = poolRetirementRepository.findByRetirementEpoch(epoch);
        if (poolsWithRetirementEpoch == null || poolsWithRetirementEpoch.isEmpty())
            return Collections.emptyList();

        Set<PoolRetirement> retiringPoolIds = new HashSet<>();
        //Check if there is any other retirement certificate with retirement epoch > epoch
        for (var poolRetirement : poolsWithRetirementEpoch) {
            var poolId = poolRetirement.getPoolId();

            //Verify if this is the recent retirement certificate for the pool
            //Find recent retirement certificate for the pool which is submitted before this epoch
            var retirementCertificate = poolRetirementRepository.findRecentPoolRetirementByEpoch(poolId, epoch - 1);
            if (retirementCertificate.isPresent() && retirementCertificate.get().getRetirementEpoch() == epoch) {
                retiringPoolIds.add(mapper.toPoolRetirement(retirementCertificate.get()));
            }
        }

        return new ArrayList<>(retiringPoolIds);
    }

    @Override
    public Optional<PoolRegistration> findPoolRegistration(String txHash, Integer certIndex) {
        return poolRegistrationRepository.findById(new PoolRegistrationId(txHash, certIndex))
                .map(mapper::toPoolRegistration);
    }

}
