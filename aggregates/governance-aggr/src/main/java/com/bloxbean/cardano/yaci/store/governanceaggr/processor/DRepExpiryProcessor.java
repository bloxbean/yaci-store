package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.adapot.event.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
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
import java.util.List;

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

        List<GovActionProposal> ratifiedOrActiveProposalsInPrevEpoch =
                proposalStateClient.getProposalsByStatusListAndEpoch(List.of(GovActionStatus.ACTIVE, GovActionStatus.RATIFIED), prevEpoch);

        final boolean proposalsExistedInPrevEpoch = !ratifiedOrActiveProposalsInPrevEpoch.isEmpty();

        final List<DRepInfo> deregisteredDRepsInPrevEpoch = dRepStorage.findDRepsByStatusAndEpoch(DRepStatus.RETIRED, prevEpoch)
                .stream().map(dRep -> new DRepInfo(dRep.getDrepHash(), dRep.getDrepId())).toList();

        final List<DRepInfo> registeredDRepsInPrevEpoch = dRepStorage.findDRepsByStatusAndEpoch(DRepStatus.REGISTERED, prevEpoch)
                .stream().map(dRep -> new DRepInfo(dRep.getDrepHash(), dRep.getDrepId())).toList();

        final List<DRepInfo> updatedDRepsInPrevEpoch = dRepStorage.findDRepsByStatusAndEpoch(DRepStatus.UPDATED, prevEpoch)
                .stream().map(dRep -> new DRepInfo(dRep.getDrepHash(), dRep.getDrepId())).toList();

        // find dReps did vote in prev epoch
        final List<String> dRepsVotedInPrevEpoch = votingAggrService.getVotesByVoterTypesInEpoch(List.of(VoterType.DREP_KEY_HASH, VoterType.DREP_SCRIPT_HASH), prevEpoch)
                .stream().map(votingProcedure -> votingProcedure.getVoterHash())
                .toList();

        final List<DRepExpiry> dRepExpiryListInPrevSnapshot = dRepExpiryStorage.findByEpoch(prevEpoch - 1);

        List<DRepExpiry> newDRepExpiryList = new ArrayList<>();

        dRepExpiryListInPrevSnapshot
                .stream()
                .filter(dRepExpiry ->
                        !deregisteredDRepsInPrevEpoch.contains(new DRepInfo(
                                dRepExpiry.getDrepHash(), dRepExpiry.getDrepId())))
                .forEach(dRepExpiry -> {
                    DRepInfo dRepInfo = new DRepInfo(dRepExpiry.getDrepHash(), dRepExpiry.getDrepId());
                    int oldDormantEpoch = dRepExpiry.getDormantEpoch();
                    int oldActiveUntil = dRepExpiry.getActiveUntil();

                    var newDRepExpiry = DRepExpiry.builder()
                            .drepId(dRepExpiry.getDrepId())
                            .drepHash(dRepExpiry.getDrepHash())
                            .epoch(prevEpoch)
                            .build();

                    if (registeredDRepsInPrevEpoch.contains(dRepInfo) || updatedDRepsInPrevEpoch.contains(dRepInfo)) {
                        newDRepExpiry.setDormantEpoch(0);
                        newDRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                    } else {
                        final boolean isDRepInactive = oldDormantEpoch + oldActiveUntil < prevEpoch;

                        if (proposalsExistedInPrevEpoch) {
                            newDRepExpiry.setDormantEpoch(0);
                            if (oldDormantEpoch > 0) {
                                newDRepExpiry.setActiveUntil(oldDormantEpoch + 1 + oldActiveUntil);
                            }
                        } else {
                            if (!isDRepInactive) { // drep is not inactive
                                newDRepExpiry.setDormantEpoch(oldDormantEpoch + 1);
                            } else {
                                newDRepExpiry.setDormantEpoch(0); // Dormant epoch is not updated for inactive DRep.
                            }
                        }

                        if (dRepsVotedInPrevEpoch.contains(dRepExpiry.getDrepHash())) {
                            newDRepExpiry.setActiveUntil(prevEpoch + drepActivity);
                        } else {
                            newDRepExpiry.setActiveUntil(oldActiveUntil);
                        }
                    }
                    newDRepExpiryList.add(newDRepExpiry);
                });

        registeredDRepsInPrevEpoch.stream()
                .filter(dRepInfo -> !dRepExpiryListInPrevSnapshot.stream()
                        .map(dRepExpiry -> new DRepInfo(dRepExpiry.getDrepHash(), dRepExpiry.getDrepId()))
                        .toList()
                        .contains(dRepInfo))
                .forEach(dRepInfo -> {
                    var dRepExpiry = DRepExpiry.builder()
                            .drepId(dRepInfo.getDRepId())
                            .drepHash(dRepInfo.getDRepHash())
                            .dormantEpoch(0)
                            .activeUntil(prevEpoch + drepActivity)
                            .epoch(prevEpoch)
                            .build();
                    newDRepExpiryList.add(dRepExpiry);
                });

        updatedDRepsInPrevEpoch.stream()
                .filter(dRepInfo -> !dRepExpiryListInPrevSnapshot.stream()
                        .map(dRepExpiry -> new DRepInfo(dRepExpiry.getDrepHash(), dRepExpiry.getDrepId()))
                        .toList()
                        .contains(dRepInfo))
                .forEach(dRepInfo -> {
                    var dRepExpiry = DRepExpiry.builder()
                            .drepId(dRepInfo.getDRepId())
                            .drepHash(dRepInfo.getDRepHash())
                            .dormantEpoch(0)
                            .activeUntil(prevEpoch + drepActivity)
                            .epoch(prevEpoch)
                            .build();
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
