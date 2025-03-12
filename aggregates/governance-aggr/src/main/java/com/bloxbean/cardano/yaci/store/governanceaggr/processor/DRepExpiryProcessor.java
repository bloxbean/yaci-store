package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.adapot.event.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRep;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepExpiry;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.VotingAggrService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepExpiryStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepStatus;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DRepExpiryProcessor {
    private final DRepExpiryStorage dRepExpiryStorage;
    private final ProposalStateClient proposalStateClient;
    private final DRepStorage dRepStorage;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final VotingAggrService votingAggrService;
    private final EpochParamStorage epochParamStorage;
    private final GovActionProposalStorage govActionProposalStorage;

    @EventListener
    @Transactional
    public void handlePreAdaPotJobProcessingEvent(PreAdaPotJobProcessingEvent event) {
        int epoch = event.getEpoch();
        int prevEpoch = epoch - 1;

        // delete if existed
        jdbcTemplate.update("delete from drep_expiry where epoch = :epoch", new MapSqlParameterSource().addValue("epoch", prevEpoch));

        var epochParamOpt = epochParamStorage.getProtocolParams(prevEpoch);
        if (epochParamOpt.isEmpty()) {
            return;
        }

        Integer drepActivity = epochParamOpt.get().getParams().getDrepActivity();
        if (drepActivity == null) {
            return;
        }

        log.info("Taking snapshot for DRep status tracking for epoch : {}", prevEpoch);

        List<GovActionProposal> ratifiedOrActiveProposalsInPrevSnapshot =
                proposalStateClient.getProposalsByStatusListAndEpoch(List.of(GovActionStatus.ACTIVE, GovActionStatus.RATIFIED), prevEpoch);

        List<com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal> govActionProposalsCreatedInPrevEpoch =
                govActionProposalStorage.findByEpoch(prevEpoch)
                        .stream().sorted(Comparator.comparingLong(com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal::getSlot)
                                .thenComparingLong(com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal::getIndex))
                        .toList();

        Long firstProposalCreatedSlot = !govActionProposalsCreatedInPrevEpoch.isEmpty() ? govActionProposalsCreatedInPrevEpoch.get(0).getSlot() : null;

        final Map<DRepInfo, Long> deregisteredDRepsInPrevEpoch = dRepStorage.findDRepsByStatusAndEpoch(DRepStatus.RETIRED, prevEpoch)
                .stream()
                .collect(Collectors.toMap(dRep -> new DRepInfo(dRep.getDrepHash(), dRep.getDrepId()), DRep::getSlot));

        final Map<DRepInfo, Long> registeredDRepsInPrevEpoch = dRepStorage.findDRepsByStatusAndEpoch(DRepStatus.REGISTERED, prevEpoch)
                .stream()
                .collect(Collectors.toMap(dRep -> new DRepInfo(dRep.getDrepHash(), dRep.getDrepId()), DRep::getSlot));

        final Map<DRepInfo, Long> updatedDRepsInPrevEpoch = dRepStorage.findDRepsByStatusAndEpoch(DRepStatus.UPDATED, prevEpoch)
                .stream()
                .collect(Collectors.toMap(dRep -> new DRepInfo(dRep.getDrepHash(), dRep.getDrepId()), DRep::getSlot));

        // find dReps did vote in prev epoch
        final Map<String, Long> dRepsVotedInPrevEpoch = votingAggrService.getVotesByVoterTypesInEpoch(List.of(VoterType.DREP_KEY_HASH, VoterType.DREP_SCRIPT_HASH), prevEpoch)
                .stream()
                .collect(Collectors.toMap(VotingProcedure::getVoterHash, VotingProcedure::getSlot));

        final List<DRepExpiry> dRepExpiryListInPrevSnapshot = dRepExpiryStorage.findByEpoch(prevEpoch - 1);

        List<DRepExpiry> newDRepExpiryList = new ArrayList<>();

        dRepExpiryListInPrevSnapshot
                .stream()
                .filter(dRepExpiry ->
                        !deregisteredDRepsInPrevEpoch.containsKey(new DRepInfo(dRepExpiry.getDrepHash(), dRepExpiry.getDrepId())))
                .forEach(dRepExpiry -> {
                    DRepInfo dRepInfo = new DRepInfo(dRepExpiry.getDrepHash(), dRepExpiry.getDrepId());
                    int oldDormantEpoch = dRepExpiry.getDormantEpochs();
                    int oldActiveUntil = dRepExpiry.getActiveUntil();

                    var newDRepExpiry = DRepExpiry.builder()
                            .drepId(dRepExpiry.getDrepId())
                            .drepHash(dRepExpiry.getDrepHash())
                            .epoch(prevEpoch)
                            .build();

                    final boolean isDRepInactive = oldDormantEpoch + oldActiveUntil < prevEpoch;

                    newDRepExpiry.setActiveUntil(oldActiveUntil);

                    if (!ratifiedOrActiveProposalsInPrevSnapshot.isEmpty() || firstProposalCreatedSlot != null) {
                        newDRepExpiry.setDormantEpochs(0);
                        if (oldDormantEpoch > 0) {
                            newDRepExpiry.setActiveUntil(oldDormantEpoch + 1 + oldActiveUntil);
                        }
                    } else if (isDRepInactive) {
                        newDRepExpiry.setDormantEpochs(0); // Dormant epoch is not updated for inactive DRep.
                    } else {
                        newDRepExpiry.setDormantEpochs(oldDormantEpoch + 1);
                    }

                    Long updatedSlot = updatedDRepsInPrevEpoch.get(dRepInfo);
                    Long votedSlot = dRepsVotedInPrevEpoch.get(dRepInfo.getDRepHash());

                    if (updatedSlot != null || votedSlot != null) {
                        newDRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                    }

                    newDRepExpiryList.add(newDRepExpiry);
                });

        // new DReps
        registeredDRepsInPrevEpoch.keySet().stream()
                .filter(dRepInfo -> dRepExpiryListInPrevSnapshot.stream()
                        .map(dRepExpiry -> new DRepInfo(dRepExpiry.getDrepHash(), dRepExpiry.getDrepId()))
                        .noneMatch(dRepInfo::equals))
                .forEach(dRepInfo -> {
                    var dRepExpiry = DRepExpiry.builder()
                            .drepId(dRepInfo.getDRepId())
                            .drepHash(dRepInfo.getDRepHash())
                            .dormantEpochs(0)
                            .epoch(prevEpoch)
                            .build();

                    if (!ratifiedOrActiveProposalsInPrevSnapshot.isEmpty()) {
                        dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                    } else if (firstProposalCreatedSlot != null) {
                        if (registeredDRepsInPrevEpoch.get(dRepInfo).compareTo(firstProposalCreatedSlot) < 0) {
                            dRepExpiry.setActiveUntil(prevEpoch + drepActivity + 1);
                        } else {
                            dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                        }
                    } else {
                        dRepExpiry.setDormantEpochs(1);
                        dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                    }

                    newDRepExpiryList.add(dRepExpiry);
                });

        // new dreps and last cert is UPDATE_CERT
        updatedDRepsInPrevEpoch.keySet().stream()
                .filter(dRepInfo -> dRepExpiryListInPrevSnapshot.stream()
                        .map(dRepExpiry -> new DRepInfo(dRepExpiry.getDrepHash(), dRepExpiry.getDrepId()))
                        .noneMatch(dRepInfo::equals))
                .forEach(dRepInfo -> {
                    var dRepExpiry = DRepExpiry.builder()
                            .drepId(dRepInfo.getDRepId())
                            .drepHash(dRepInfo.getDRepHash())
                            .dormantEpochs(0)
                            .epoch(prevEpoch)
                            .build();

                    if (!ratifiedOrActiveProposalsInPrevSnapshot.isEmpty()) {
                        dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                    } else if (firstProposalCreatedSlot != null) {
                        if (updatedDRepsInPrevEpoch.get(dRepInfo).compareTo(firstProposalCreatedSlot) < 0) {
                            dRepExpiry.setActiveUntil(prevEpoch + drepActivity + 1);
                        } else {
                            dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                        }
                    } else {
                        dRepExpiry.setDormantEpochs(1);
                        dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                    }

                    newDRepExpiryList.add(dRepExpiry);
                });

        if (!newDRepExpiryList.isEmpty()) {
            dRepExpiryStorage.save(newDRepExpiryList);
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    static class DRepInfo {
        String dRepHash;
        String dRepId;
    }
}
