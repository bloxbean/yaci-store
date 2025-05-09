package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobExtraInfo;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.events.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governance.domain.DRep;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepExpiry;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.VotingAggrService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepExpiryStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.DRepStorage;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepStatus;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.governanceaggr.GovernanceAggrConfiguration.STORE_GOVERNANCEAGGR_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(value = STORE_GOVERNANCEAGGR_ENABLED, defaultValue = false)
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
    private final AdaPotJobStorage adaPotJobStorage;

    @EventListener
    @Transactional
    public void handlePreAdaPotJobProcessingEvent(PreAdaPotJobProcessingEvent event) {
        var start = Instant.now();

        // Taking snapshot for DRep expiry
        if (eraService.getEraForEpoch(event.getEpoch() - 1).getValue() < Era.Conway.getValue()) {
            return;
        }

        int epoch = event.getEpoch();
        int prevEpoch = epoch - 1;

        // delete if existed
        jdbcTemplate.update("delete from drep_expiry where epoch = :epoch", new MapSqlParameterSource().addValue("epoch", epoch));

        var epochParamOpt = epochParamStorage.getProtocolParams(epoch);
        if (epochParamOpt.isEmpty()) {
            return;
        }

        Integer drepActivity = epochParamOpt.get().getParams().getDrepActivity();
        if (drepActivity == null) {
            return;
        }

        log.info("Taking snapshot for DRep expiry for epoch : {}", epoch);

        // get ratified or active proposals in prev proposal status snapshot (the epoch = current epoch - 1)
        List<GovActionProposal> ratifiedOrActiveProposalsInPrevProposalStatusSnapshot =
                proposalStateClient.getProposalsByStatusListAndEpoch(List.of(GovActionStatus.ACTIVE, GovActionStatus.RATIFIED), prevEpoch);

        // get governance action proposals that were created in previous epoch, at this point of time they're not in prev proposal status snapshot
        List<com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal> govActionProposalsCreatedInPrevEpoch =
                govActionProposalStorage.findByEpoch(prevEpoch)
                        .stream().sorted(Comparator.comparingLong(com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal::getSlot)
                                .thenComparingLong(com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal::getIndex))
                        .toList();

        // The slot the first proposal was created slot in the previous epoch
        Long firstProposalCreatedSlotInPrevEpoch = !govActionProposalsCreatedInPrevEpoch.isEmpty() ? govActionProposalsCreatedInPrevEpoch.get(0).getSlot() : null;

        // get last epoch (before the prev epoch) that had ratified or active proposals
        Optional<Integer> lastEpochHadRatifiedOrActiveProposalsOpt = proposalStateClient.getLatestEpochWithStatusBefore(List.of(GovActionStatus.ACTIVE, GovActionStatus.RATIFIED), prevEpoch);

        Optional<CardanoEra> conwayEra = eraService.getEras()
                .stream().filter(cardanoEra -> cardanoEra.getEra().equals(Era.Conway))
                .findFirst();

        int firstEpochNoInConway = conwayEra.map(cardanoEra ->
                eraService.getEpochNo(cardanoEra.getEra(), cardanoEra.getStartSlot())).orElse(0);

        int dormantEpochsCount = lastEpochHadRatifiedOrActiveProposalsOpt
                .map(e -> prevEpoch - e).orElseGet(() ->  prevEpoch - firstEpochNoInConway);

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
        final List<String> dRepsVotedInPrevEpoch = votingAggrService.getVotesByVoterTypesInEpoch(List.of(VoterType.DREP_KEY_HASH, VoterType.DREP_SCRIPT_HASH), prevEpoch)
                .stream()
                .map(VotingProcedure::getVoterHash)
                .collect(Collectors.toList());

        final List<DRepExpiry> dRepExpiryListInPrevSnapshot = dRepExpiryStorage.findByEpoch(epoch - 1);

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
                    .epoch(epoch)
                    .build();

            newDRepExpiry.setActiveUntil(oldActiveUntil);
            if ((ratifiedOrActiveProposalsInPrevProposalStatusSnapshot.isEmpty() && firstProposalCreatedSlotInPrevEpoch != null)) {
                // extend DRep expiry
                newDRepExpiry.setActiveUntil(oldActiveUntil + dormantEpochsCount + 1);
            }

            // the case DRep is updated or DRep did vote in the previous epoch
            if (updatedDRepsInPrevEpoch.get(dRepInfo) != null || dRepsVotedInPrevEpoch.contains(dRepInfo.getDRepHash())) {
                newDRepExpiry.setActiveUntil(prevEpoch + drepActivity);
            }

            newDRepExpiryList.add(newDRepExpiry);
        }

        // new DReps and last cert type is REG_DREP_CERT
        registeredDRepsInPrevEpoch.keySet().stream()
                .filter(dRepInfo -> dRepExpiryListInPrevSnapshot.stream()
                        .map(dRepExpiry -> new DRepInfo(dRepExpiry.getDrepHash(), dRepExpiry.getDrepId()))
                        .noneMatch(dRepInfo::equals))
                .forEach(dRepInfo -> {
                    var dRepExpiry = DRepExpiry.builder()
                            .drepId(dRepInfo.getDRepId())
                            .drepHash(dRepInfo.getDRepHash())
                            .epoch(epoch)
                            .build();

                    if (!ratifiedOrActiveProposalsInPrevProposalStatusSnapshot.isEmpty()) {
                        dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                    } else if (firstProposalCreatedSlotInPrevEpoch != null) {
                        if (registeredDRepsInPrevEpoch.get(dRepInfo).compareTo(firstProposalCreatedSlotInPrevEpoch) < 0) { // the DRep was updated before the first proposal was created
                            dRepExpiry.setActiveUntil(prevEpoch + drepActivity + 1); // in this case, we plus 1, which is the value of the dormant epoch counter
                        } else {
                            dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                        }
                    } else {
                        dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                    }

                    newDRepExpiryList.add(dRepExpiry);
                });

        // new DReps and last cert is UPDATE_DREP_CERT
        updatedDRepsInPrevEpoch.keySet().stream()
                .filter(dRepInfo -> dRepExpiryListInPrevSnapshot.stream()
                        .map(dRepExpiry -> new DRepInfo(dRepExpiry.getDrepHash(), dRepExpiry.getDrepId()))
                        .noneMatch(dRepInfo::equals))
                .forEach(dRepInfo -> {
                    var dRepExpiry = DRepExpiry.builder()
                            .drepId(dRepInfo.getDRepId())
                            .drepHash(dRepInfo.getDRepHash())
                            .epoch(epoch)
                            .build();

                    if (!ratifiedOrActiveProposalsInPrevProposalStatusSnapshot.isEmpty()) {
                        dRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                    } else if (firstProposalCreatedSlotInPrevEpoch != null) {
                        if (updatedDRepsInPrevEpoch.get(dRepInfo).compareTo(firstProposalCreatedSlotInPrevEpoch) < 0) { // the DRep was updated before the first proposal was created
                            dRepExpiry.setActiveUntil(prevEpoch + drepActivity + 1); // in this case, we plus 1, which is the value of the dormant epoch counter
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

        var end = Instant.now();

        adaPotJobStorage.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, epoch)
                .ifPresent(adaPotJob -> {
                    AdaPotJobExtraInfo extraInfo = adaPotJob.getExtraInfo();

                    if (extraInfo == null) {
                        extraInfo = AdaPotJobExtraInfo.builder()
                                .drepExpiryCalcTime(0L)
                                .govActionStatusCalcTime(0L)
                                .build();
                    }

                    extraInfo.setDrepExpiryCalcTime(end.toEpochMilli() - start.toEpochMilli());
                    adaPotJob.setExtraInfo(extraInfo);

                    adaPotJobStorage.save(adaPotJob);
                });

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
