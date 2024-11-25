package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.domain.event.DRepRegistrationEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.event.DRepVotingEvent;
import com.bloxbean.cardano.yaci.store.governance.storage.VotingProcedureStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRep;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
// Todo: handle by query during epoch transition
public class DRepStatusProcessor {
    private final DRepStorage dRepStorage;
    private final EpochParamStorage epochParamStorage;
    private final VotingProcedureStorage votingProcedureStorage;

    private final List<DRepRegistrationEvent> dRepRegistrationsCache = Collections.synchronizedList(new ArrayList<>());
    private final List<DRepVotingEvent> dRepVotingCache = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, String> inactiveDRepCache = new ConcurrentHashMap<>();

    @EventListener
    public void processDRepRegistrationEvent(DRepRegistrationEvent dRepRegistrationEvent) {
        dRepRegistrationsCache.add(dRepRegistrationEvent);
    }

    @EventListener
    @Transactional
    public void handleCommitEventToProcessDRepStatus(PreCommitEvent preCommitEvent) {
        try {
            dRepRegistrationsCache.sort(Comparator.comparingLong(e -> e.getMetadata().getSlot()));
            List<DRep> dReps = new ArrayList<>();

            for (var drepRegistrationEvent : dRepRegistrationsCache) {
                List<DRepRegistration> dRepRegistrations = drepRegistrationEvent.getDRepRegistrations();
                EventMetadata metadata = drepRegistrationEvent.getMetadata();
                dRepRegistrations.sort(Comparator.comparingLong(DRepRegistration::getTxIndex)
                        .thenComparingLong(DRepRegistration::getCertIndex));

                for (var drepRegistration : dRepRegistrations) {
                    var dRepBuilder = DRep.builder()
                            .drepId(drepRegistration.getDrepId())
                            .drepHash(drepRegistration.getDrepHash())
                            .txHash(drepRegistration.getTxHash())
                            .certIndex((int) drepRegistration.getCertIndex())
                            .txIndex(drepRegistration.getTxIndex())
                            .certType(drepRegistration.getType())
                            .deposit(drepRegistration.getDeposit())
                            .epoch(drepRegistration.getEpoch())
                            .activeEpoch(metadata.getEpochNumber())
                            .slot(metadata.getSlot())
                            .blockHash(metadata.getBlockHash());

                    if (drepRegistration.getType() == CertificateType.REG_DREP_CERT) {
                        dRepBuilder.status(DRepStatus.ACTIVE)
                                .registrationSlot(drepRegistration.getSlot());
                    } else {
                        var recentDRepOpt = dRepStorage.findRecentDRepRegistration(drepRegistration.getDrepId(), metadata.getEpochNumber());

                        if (recentDRepOpt.isEmpty()) {
                            var regisDRep = dReps.stream().filter(dRep -> dRep.getDrepId().equals(drepRegistration.getDrepId())).findFirst();

                            if (regisDRep.isEmpty()) {
                                log.error("Cannot find recent dRep registration");
                            } else {
                                dRepBuilder.registrationSlot(regisDRep.get().getRegistrationSlot());
                                dRepBuilder.deposit(regisDRep.get().getDeposit());
                            }
                        } else {
                            dRepBuilder.registrationSlot(recentDRepOpt.get().getRegistrationSlot());
                            dRepBuilder.deposit(recentDRepOpt.get().getDeposit());
                        }

                        if (drepRegistration.getType() == CertificateType.UPDATE_DREP_CERT) {
                            dRepBuilder.status(DRepStatus.ACTIVE);
                        } else if (drepRegistration.getType() == CertificateType.UNREG_DREP_CERT) {
                            dRepBuilder.status(DRepStatus.RETIRED)
                                    .retireEpoch(metadata.getEpochNumber());
                        }
                    }
                    dReps.add(dRepBuilder.build());
                }
            }

            if (!dReps.isEmpty()) {
                dRepStorage.saveAll(dReps);
            }

            // handle active again case by voting
            dRepVotingCache.sort(Comparator.comparingLong(e -> e.getMetadata().getSlot()));

            if (!inactiveDRepCache.isEmpty()) {
                // TODO: optimize
                var inactiveDRep = dRepStorage.findDRepsByStatus(DRepStatus.INACTIVE, 0, Integer.MAX_VALUE, Order.desc);

                if (inactiveDRep.isEmpty()) {
                    return;
                }
                inactiveDRep.forEach(dRep -> inactiveDRepCache.put(dRep.getDrepHash(), dRep.getDrepId()));
            }

            List<DRep> activeDReps = new ArrayList<>();

            for (var dRepVotingEvent : dRepVotingCache) {
                var votingProcedures = dRepVotingEvent.getVotingProcedures();
                EventMetadata metadata = dRepVotingEvent.getMetadata();

                for (var voting : votingProcedures) {
                    if (inactiveDRepCache.containsKey(voting.getVoterHash())) {
                        var dRepId = inactiveDRepCache.get(voting.getVoterHash());
                        var recentDRepOpt = dRepStorage.findRecentDRepRegistration(dRepId, metadata.getEpochNumber());

                        if (recentDRepOpt.isEmpty()) {
                            log.error("Cannot find recent dRep registration");
                        } else {
                            activeDReps.add(
                                    DRep.builder()
                                            .drepId(dRepId)
                                            .drepHash(voting.getVoterHash())
                                            .status(DRepStatus.ACTIVE)
                                            .deposit(recentDRepOpt.get().getDeposit())
                                            .activeEpoch(metadata.getEpochNumber())
                                            .epoch(metadata.getEpochNumber())
                                            .slot(metadata.getSlot())
                                            .blockNumber(metadata.getBlock())
                                            .blockHash(metadata.getBlockHash())
                                            .certIndex(-1)
                                            .txHash("")
                                            .txIndex(-1)
                                            .build()
                            );
                            inactiveDRepCache.remove(voting.getVoterHash());
                        }
                    }
                }
            }

            if (!activeDReps.isEmpty()) {
                dRepStorage.saveAll(activeDReps);
            }
        } finally {
            dRepRegistrationsCache.clear();
        }
    }

    @EventListener
    @Transactional
    public void handleEpochChangeEventToProcessDRepStatus(PreEpochTransitionEvent preEpochTransitionEvent) {
        // get current epoch param -> get drepActivity value
        // check dRep (active, do not vote in {drepActivity} epoch)
        // if not vote -> set inactive
        Integer prevEpoch = preEpochTransitionEvent.getPreviousEpoch();

        if (prevEpoch == null) {
            return;
        }
        var eventMetadata = preEpochTransitionEvent.getMetadata();

        var epochParamOpt = epochParamStorage.getProtocolParams(prevEpoch);
        if (epochParamOpt.isEmpty()) {
            return;
        }

        Integer dRepActivity = epochParamOpt.get().getParams().getDrepActivity();
        if (dRepActivity == null) {
            return;
        }

        // TODO: refactor and optimize
        List<VotingProcedure> votesByDRepInDRepActivityEpoch =
                Stream.of(VoterType.DREP_KEY_HASH, VoterType.DREP_SCRIPT_HASH)
                        .flatMap(voterType -> votingProcedureStorage.findByVoterTypeAndEpochIsGreaterThanEqual(voterType, prevEpoch - dRepActivity + 1).stream())
                        .toList();

        List<String> dRepHashListInVotes = votesByDRepInDRepActivityEpoch.stream()
                .map(VotingProcedure::getVoterHash)
                .distinct()
                .toList();

        // find all dReps which do not vote in {drepActivity} epoch
        Map<Tuple<String, String>, BigInteger> dRepsNotVoteMap = new HashMap<>();

        int page = 0;
        int count = 100;

        while (true) {
            List<DRep> dReps = dRepStorage.findDRepsByStatus(DRepStatus.ACTIVE, page, count, Order.desc);
            if (dReps.isEmpty()) {
                break;
            }
            dReps.stream()
                    .filter(dRep -> !dRepHashListInVotes.contains(dRep.getDrepHash())
                            && dRep.getEpoch() + dRepActivity < eventMetadata.getEpochNumber())
                    .forEach(dRep -> dRepsNotVoteMap.put(new Tuple<>(dRep.getDrepId(), dRep.getDrepHash()), dRep.getDeposit()));
            page++;
        }

        List<DRep> inactiveDReps = dRepsNotVoteMap.keySet().stream().map(
                tuple -> DRep.builder()
                        .drepId(tuple._1)
                        .drepHash(tuple._2)
                        .status(DRepStatus.INACTIVE)
                        .deposit(dRepsNotVoteMap.get(tuple))
                        .inactiveEpoch(preEpochTransitionEvent.getEpoch())
                        .epoch(preEpochTransitionEvent.getEpoch())
                        .slot(eventMetadata.getSlot())
                        .blockNumber(eventMetadata.getBlock())
                        .blockHash(eventMetadata.getBlockHash())
                        .certIndex(-1)
                        .txHash("")
                        .txIndex(-1)
                        .build()
        ).collect(Collectors.toList());

        if (!inactiveDReps.isEmpty()) {
            dRepStorage.saveAll(inactiveDReps);
        }

        dRepsNotVoteMap.forEach((key, value) -> inactiveDRepCache.put(key._2, key._1));
    }

    // handle active again case
    @EventListener
    public void processDRepVotingEvent(DRepVotingEvent dRepVotingEvent) {
        dRepVotingCache.add(dRepVotingEvent);
    }

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = dRepStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} drep records", count);

        inactiveDRepCache.clear();
    }
}
