package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.adapot.event.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.GovernanceAggrProperties;
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

import java.util.*;
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
    private final EraService eraService;
    private final EpochParamStorage epochParamStorage;
    private final GovActionProposalStorage govActionProposalStorage;
    private final GovernanceAggrProperties governanceAggrProperties;

    @EventListener
    @Transactional
    public void handlePreAdaPotJobProcessingEvent(PreAdaPotJobProcessingEvent event) {
        if (!governanceAggrProperties.isEnabled())
            return;

        if (eraService.getEraForEpoch(event.getEpoch() - 1).getValue() < Era.Conway.getValue()) {
            return;
        }

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
        int dormantEpochs = 0;

        List<GovActionProposal> ratifiedOrActiveProposalsInPrevProposalStatusSnapshot =
                proposalStateClient.getProposalsByStatusListAndEpoch(List.of(GovActionStatus.ACTIVE, GovActionStatus.RATIFIED), prevEpoch);

        List<com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal> govActionProposalsCreatedInPrevEpoch =
                govActionProposalStorage.findByEpoch(prevEpoch)
                        .stream().sorted(Comparator.comparingLong(com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal::getSlot)
                                .thenComparingLong(com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal::getIndex))
                        .toList();

        Long firstProposalCreatedSlotInPrevEpoch = !govActionProposalsCreatedInPrevEpoch.isEmpty() ? govActionProposalsCreatedInPrevEpoch.get(0).getSlot() : null;
        Optional<Integer> dormantEpochsOpt = proposalStateClient.getLatestEpochWithStatusBefore(List.of(GovActionStatus.ACTIVE, GovActionStatus.RATIFIED), prevEpoch);

        if (dormantEpochsOpt.isEmpty()) {
            // TODO
        } else
            dormantEpochs = dormantEpochsOpt.get();

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

        for (var dRepExpiry : dRepExpiryListInPrevSnapshot) {
            if (deregisteredDRepsInPrevEpoch.containsKey(new DRepInfo(dRepExpiry.getDrepHash(), dRepExpiry.getDrepId()))) {
                continue;
            }

            DRepInfo dRepInfo = new DRepInfo(dRepExpiry.getDrepHash(), dRepExpiry.getDrepId());
            int oldActiveUntil = dRepExpiry.getActiveUntil();

            var newDRepExpiry = DRepExpiry.builder()
                    .drepId(dRepExpiry.getDrepId())
                    .drepHash(dRepExpiry.getDrepHash())
                    .epoch(prevEpoch)
                    .build();

            newDRepExpiry.setActiveUntil(oldActiveUntil);
            if ((!ratifiedOrActiveProposalsInPrevProposalStatusSnapshot.isEmpty() || firstProposalCreatedSlotInPrevEpoch != null)
                    && dormantEpochs > 0) {
                    // extend expiry
                    newDRepExpiry.setActiveUntil(oldActiveUntil + dormantEpochs);
                }

            Long updatedSlot = updatedDRepsInPrevEpoch.get(dRepInfo);

            Long votedSlot = dRepsVotedInPrevEpoch.get(dRepInfo.getDRepHash());

            if (updatedSlot != null || votedSlot != null) {
                newDRepExpiry.setActiveUntil(prevEpoch + drepActivity);
            }

            newDRepExpiryList.add(newDRepExpiry);
        }

        // new DReps
        registeredDRepsInPrevEpoch.keySet().stream()
                .filter(dRepInfo -> dRepExpiryListInPrevSnapshot.stream()
                        .map(dRepExpiry -> new DRepInfo(dRepExpiry.getDrepHash(), dRepExpiry.getDrepId()))
                        .noneMatch(dRepInfo::equals))
                .forEach(dRepInfo -> {
                    var dRepExpiry = DRepExpiry.builder()
                            .drepId(dRepInfo.getDRepId())
                            .drepHash(dRepInfo.getDRepHash())
                            .epoch(prevEpoch)
                            .build();

                    if (!ratifiedOrActiveProposalsInPrevProposalStatusSnapshot.isEmpty()) {
                        dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                    } else if (firstProposalCreatedSlotInPrevEpoch != null) {
                        if (registeredDRepsInPrevEpoch.get(dRepInfo).compareTo(firstProposalCreatedSlotInPrevEpoch) < 0) {
                            dRepExpiry.setActiveUntil(prevEpoch + drepActivity + 1);
                        } else {
                            dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                        }
                    } else {
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
                            .epoch(prevEpoch)
                            .build();

                    if (!ratifiedOrActiveProposalsInPrevProposalStatusSnapshot.isEmpty()) {
                        dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                    } else if (firstProposalCreatedSlotInPrevEpoch != null) {
                        if (updatedDRepsInPrevEpoch.get(dRepInfo).compareTo(firstProposalCreatedSlotInPrevEpoch) < 0) {
                            dRepExpiry.setActiveUntil(prevEpoch + drepActivity + 1);
                        } else {
                            dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                        }
                    } else {
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
