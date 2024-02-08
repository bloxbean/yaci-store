package com.bloxbean.cardano.yaci.store.staking.processor;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.staking.domain.Pool;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolStatusType;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.domain.event.*;
import com.bloxbean.cardano.yaci.store.staking.service.DepositParamService;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PoolStatusProcessor {
    private final PoolStorage poolStorage;
    private final DepositParamService depositParamService;
    private final ApplicationEventPublisher publisher;

    private List<StakeRegistrationDetail> stakeKeyRegCache = Collections.synchronizedList(new ArrayList<>());
    private List<StakeRegistrationDetail> stakeKeyDeRegCache = Collections.synchronizedList(new ArrayList<>());

    private List<PoolRegistrationEvent> poolRegistrationsCache = Collections.synchronizedList(new ArrayList<>());
    private List<PoolRetirementEvent> poolRetirementsCache = Collections.synchronizedList(new ArrayList<>());

    @EventListener
    @Transactional
    public void processStakeKeyRegDeregs(StakeRegDeregEvent stakeRegDeregEvent) {
        if (stakeRegDeregEvent.getStakeRegistrationDetails().isEmpty())
            return;

        for (StakeRegistrationDetail stakeRegistrationDetail : stakeRegDeregEvent.getStakeRegistrationDetails()) {
            if (stakeRegistrationDetail.getType() == CertificateType.STAKE_REGISTRATION) {
                stakeKeyRegCache.add(stakeRegistrationDetail);
            } else if (stakeRegistrationDetail.getType() == CertificateType.STAKE_DEREGISTRATION) {
                stakeKeyDeRegCache.add(stakeRegistrationDetail);
            }
        }
    }

    @EventListener
    @Transactional
    public void processPoolRegistrationEvent(PoolRegistrationEvent poolRegistrationEvent) {
        poolRegistrationsCache.add(poolRegistrationEvent);
    }

    @EventListener
    @Transactional
    public void processPoolRetirementEvent(PoolRetirementEvent poolRetirementEvent) {
        poolRetirementsCache.add(poolRetirementEvent);
    }

    @EventListener
    @Transactional
    public void handleCommitEventToProcessPoolStatus(CommitEvent commitEvent) {
        //sort based on slot event.getMetadata().getSlot()
        poolRegistrationsCache.sort(Comparator.comparingLong(e -> e.getMetadata().getSlot()));
        poolRetirementsCache.sort(Comparator.comparingLong(e -> e.getMetadata().getSlot()));

        List<Pool> poolRegistrations = new ArrayList<>();
        for (var poolRegistrationEvent : poolRegistrationsCache) {
            var poolRegUpdateList = handlePoolRegistration(poolRegistrationEvent);
            var blockPoolRegistraions = poolRegUpdateList.stream().filter(p -> p.getStatus() == PoolStatusType.REGISTRATION).toList();
            if (!blockPoolRegistraions.isEmpty())
                poolRegistrations.addAll(blockPoolRegistraions);
        }

        for (var poolRetirementEvent : poolRetirementsCache) {
            handlePoolRetirement(poolRetirementEvent);
        }

        var stakingDepositEvent = new StakingDepositEvent(commitEvent.getMetadata(), stakeKeyRegCache.size(),
                stakeKeyDeRegCache.size(), poolRegistrations.size());

        //Publish StakingDepositEvent to trigger adapot deposit calculation
        publisher.publishEvent(stakingDepositEvent);

        poolRegistrationsCache.clear();
        poolRetirementsCache.clear();

        stakeKeyRegCache.clear();
        stakeKeyDeRegCache.clear();
    }


    public List<Pool> handlePoolRegistration(PoolRegistrationEvent poolRegistrationEvent) {
        var metadata = poolRegistrationEvent.getMetadata();
        BigInteger ppPoolDeposit = depositParamService.getPoolDeposit(metadata.getEpochNumber());

        List<Pool> poolRegsUpdates = new ArrayList<>();
        for (var poolRegistration : poolRegistrationEvent.getPoolRegistrations()) {
            var poolId = poolRegistration.getPoolId();
            var poolRegistrationOpt = poolStorage.findRecentPoolRegistration(poolId, metadata.getEpochNumber());
            var poolRetiredOpt = poolStorage.findRecentPoolRetired(poolId, metadata.getEpochNumber());

            var poolRegistrationSlot = poolRegistrationOpt.map(Pool::getSlot).orElse(-1L);
            var poolRetiredSlot = poolRetiredOpt.map(Pool::getSlot).orElse(-1L);

            if (poolRegistrationSlot == -1 ||
                    (poolRegistrationSlot != -1 && poolRetiredSlot > poolRegistrationSlot)) { //No registration found || Pool already retired
                var poolStatusRegistration = Pool.builder()
                        .poolId(poolId)
                        .txHash(poolRegistration.getTxHash())
                        .certIndex(poolRegistration.getCertIndex())
                        .status(PoolStatusType.REGISTRATION)
                        .amount(ppPoolDeposit)
                        .epoch(metadata.getEpochNumber())
                        .slot(metadata.getSlot())
                        .blockNumber(metadata.getBlock())
                        .blockHash(metadata.getBlockHash())
                        .blockTime(metadata.getBlockTime())
                        .build();

                poolRegsUpdates.add(poolStatusRegistration);
                poolStorage.save(List.of(poolStatusRegistration));
            } else {
                var poolStatusUpdate = Pool.builder()
                        .poolId(poolId)
                        .txHash(poolRegistration.getTxHash())
                        .certIndex(poolRegistration.getCertIndex())
                        .status(PoolStatusType.UPDATE)
                        .amount(BigInteger.ZERO)
                        .epoch(metadata.getEpochNumber())
                        .slot(metadata.getSlot())
                        .blockNumber(metadata.getBlock())
                        .blockHash(metadata.getBlockHash())
                        .blockTime(metadata.getBlockTime())
                        .build();

                poolRegsUpdates.add(poolStatusUpdate);
                poolStorage.save(List.of(poolStatusUpdate));
            }
        }

        return poolRegsUpdates;
    }

    public void handlePoolRetirement(PoolRetirementEvent poolRetirementEvent) {
        var metadata = poolRetirementEvent.getMetadata();

        for (var poolRetirement : poolRetirementEvent.getPoolRetirements()) {
            var poolId = poolRetirement.getPoolId();

            var poolRetiring = Pool.builder()
                    .poolId(poolId)
                    .txHash(poolRetirement.getTxHash())
                    .certIndex(poolRetirement.getCertIndex())
                    .status(PoolStatusType.RETIRING)
                    .amount(BigInteger.ZERO)
                    .epoch(metadata.getEpochNumber())
                    .retireEpoch(poolRetirement.getRetirementEpoch())
                    .slot(metadata.getSlot())
                    .blockNumber(metadata.getBlock())
                    .blockHash(metadata.getBlockHash())
                    .blockTime(metadata.getBlockTime())
                    .build();

            poolStorage.save(List.of(poolRetiring));
        }
    }

    @EventListener
    @Transactional
    public void handlePoolRetirementsDuringEpochChange(EpochChangeEvent epochChangeEvent) {
        var metadata = epochChangeEvent.getEventMetadata();

        log.info("Processing pool retirement for epoch: " + epochChangeEvent.getEpoch());

        var newEpoch = epochChangeEvent.getEpoch();

        //Find pool retirements
        List<Pool> retiringPools = poolStorage.findRetiringPools(newEpoch);
        if (retiringPools.size() == 0) {
            log.info("Retiring pools size : " + retiringPools.size() + ", epoch: " + newEpoch);
            return;
        }

        var poolDeposit = depositParamService.getPoolDeposit(newEpoch); //TODO -- Get amount from pool's registration

        List<Pool> retiredPools = new ArrayList<>();
        //Create retired pool status
        for (Pool retirement : retiringPools) {
            var retiredPool = Pool.builder()
                    .poolId(retirement.getPoolId())
                    .txHash(retirement.getTxHash())
                    .certIndex(retirement.getCertIndex())
                    .status(PoolStatusType.RETIRED)
                    .amount(poolDeposit.negate())
                    .epoch(newEpoch)
                    .retireEpoch(newEpoch)
                    .slot(metadata.getSlot())
                    .blockHash(metadata.getBlockHash())
                    .blockNumber(metadata.getBlock())
                    .blockTime(metadata.getBlockTime())
                    .build();

            retiredPools.add(retiredPool);
            poolStorage.save(List.of(retiredPool));
        }

        //Publish PoolRetiredEvent to trigger adapot refund calculation
        publisher.publishEvent(new PoolRetiredEvent(metadata, retiredPools));
    }
}
