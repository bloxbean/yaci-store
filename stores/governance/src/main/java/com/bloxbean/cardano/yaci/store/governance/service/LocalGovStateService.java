package com.bloxbean.cardano.yaci.store.governance.service;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.CredentialType;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.core.model.governance.Committee;
import com.bloxbean.cardano.yaci.core.model.governance.Constitution;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.GovStateQuery;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.GovStateResult;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.Proposal;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.core.service.TipFinderService;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.*;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
@Slf4j
public class LocalGovStateService {
    private final LocalClientProvider localClientProvider;
    private final LocalStateQueryClient localStateQueryClient;
    private final LocalGovActionProposalStatusRepository localGovActionProposalStatusRepository;
    private final LocalConstitutionRepository localConstitutionRepository;
    private final LocalCommitteeMemberRepository localCommitteeMemberRepository;
    private final LocalCommitteeRepository localCommitteeRepository;
    private final GovActionProposalRepository govActionProposalRepository;
    private final LocalTreasuryWithdrawalRepository localTreasuryWithdrawalRepository;
    private final TipFinderService tipFinderService;
    private final EraService eraService;

    public LocalGovStateService(LocalClientProvider localClientProvider,
                                LocalGovActionProposalStatusRepository localGovActionProposalStatusRepository,
                                LocalConstitutionRepository localConstitutionRepository,
                                LocalCommitteeMemberRepository localCommitteeMemberRepository,
                                LocalCommitteeRepository localCommitteeRepository,
                                GovActionProposalRepository govActionProposalRepository,
                                LocalTreasuryWithdrawalRepository localTreasuryWithdrawalRepository,
                                TipFinderService tipFinderService,
                                EraService eraService) {
        this.localClientProvider = localClientProvider;
        this.localStateQueryClient = localClientProvider.getLocalStateQueryClient();
        this.localGovActionProposalStatusRepository = localGovActionProposalStatusRepository;
        this.localConstitutionRepository = localConstitutionRepository;
        this.localCommitteeMemberRepository = localCommitteeMemberRepository;
        this.localCommitteeRepository = localCommitteeRepository;
        this.localTreasuryWithdrawalRepository = localTreasuryWithdrawalRepository;
        this.govActionProposalRepository = govActionProposalRepository;
        this.tipFinderService = tipFinderService;
        this.eraService = eraService;
        log.info("LocalGovActionStateService initialized >>>");
    }

    @Transactional
    public synchronized void fetchAndSetGovState() {
        getGovStateFromNode()
                .doOnError(throwable -> log.error("Local gov state sync error {}", throwable.getMessage()))
                .subscribe(this::processGovStateResult);
    }

    private void processGovStateResult(GovStateResult govStateResult) {
        Tip tip = tipFinderService.getTip().block();
        Integer currentEpoch = eraService.getEpochNo(com.bloxbean.cardano.yaci.core.model.Era.Conway, tip.getPoint().getSlot());
        Long slot = tip.getPoint().getSlot();

        List<LocalGovActionProposalStatusEntity> govActionsInPrevEpoch = localGovActionProposalStatusRepository
                .findByEpochAndStatusIn(currentEpoch - 1, List.of(GovActionStatus.EXPIRED, GovActionStatus.RATIFIED));

        List<LocalGovActionProposalStatusEntity> entitiesToSave = new ArrayList<>();

        processExpiredProposals(govStateResult, currentEpoch, slot, entitiesToSave);
        processRatifiedProposals(govStateResult, currentEpoch, slot, entitiesToSave);
        processEnactedAndDroppedProposals(govActionsInPrevEpoch, govStateResult, currentEpoch, slot, entitiesToSave);

        if (!entitiesToSave.isEmpty()) {
            localGovActionProposalStatusRepository.saveAll(entitiesToSave);
        }

        updateConstitution(govStateResult, currentEpoch, slot);
        updateCommittee(govStateResult, currentEpoch, slot);
        updateCommitteeMembers(govStateResult, currentEpoch, slot);
        processTreasuryWithdrawals(entitiesToSave.stream().filter(entity -> entity.getStatus().equals(GovActionStatus.ENACTED))
                        .map(entity -> GovActionId.builder()
                                .transactionId(entity.getGovActionTxHash())
                                .gov_action_index((int) entity.getGovActionIndex())
                                .build()).toList(),
                currentEpoch, slot);
    }

    private void processExpiredProposals(GovStateResult govStateResult, Integer currentEpoch, Long slot, List<LocalGovActionProposalStatusEntity> entitiesToSave) {
        List<GovActionId> expiredGovActions = govStateResult.getNextRatifyState().getExpiredGovActions();
        expiredGovActions.forEach(govActionId -> entitiesToSave.add(buildLocalGovActionProposalEntity(govActionId, GovActionStatus.EXPIRED, currentEpoch, slot)));
    }

    private void processRatifiedProposals(GovStateResult govStateResult, Integer currentEpoch, Long slot, List<LocalGovActionProposalStatusEntity> entitiesToSave) {
        List<GovActionId> ratifiedGovActions = govStateResult.getNextRatifyState().getEnactedGovActions()
                .stream()
                .map(Proposal::getGovActionId)
                .toList();
        ratifiedGovActions.forEach(govActionId -> entitiesToSave.add(buildLocalGovActionProposalEntity(govActionId, GovActionStatus.RATIFIED, currentEpoch, slot)));
    }

    private void processEnactedAndDroppedProposals(List<LocalGovActionProposalStatusEntity> govActionsInPrevEpoch, GovStateResult govStateResult, Integer currentEpoch, Long slot, List<LocalGovActionProposalStatusEntity> entitiesToSave) {
        List<GovActionId> proposalsInNextRatify = govStateResult.getProposals().stream().map(Proposal::getGovActionId).toList();

        govActionsInPrevEpoch.forEach(govAction -> {
            GovActionId govActionId = GovActionId.builder()
                    .transactionId(govAction.getGovActionTxHash())
                    .gov_action_index((int) govAction.getGovActionIndex())
                    .build();

            if (govAction.getStatus().equals(GovActionStatus.RATIFIED) && !proposalsInNextRatify.contains(govActionId)) {
                entitiesToSave.add(buildLocalGovActionProposalEntity(govActionId, GovActionStatus.ENACTED, currentEpoch, slot));
            }

            if (govAction.getStatus().equals(GovActionStatus.EXPIRED) && !proposalsInNextRatify.contains(govActionId)) {
                entitiesToSave.add(buildLocalGovActionProposalEntity(govActionId, GovActionStatus.DROPPED, currentEpoch, slot));
            }
        });
    }

    private void updateConstitution(GovStateResult govStateResult, Integer currentEpoch, Long slot) {
        Constitution constitution = govStateResult.getConstitution();
        LocalConstitutionEntity localConstitutionEntity = buildLocalConstitutionEntity(constitution, currentEpoch, slot);
        localConstitutionRepository.save(localConstitutionEntity);
    }

    private void updateCommittee(GovStateResult govStateResult, Integer currentEpoch, Long slot) {
        Committee committee = govStateResult.getCommittee();
        LocalCommitteeEntity localCommitteeEntity = buildLocalCommitteeEntity(committee, currentEpoch, slot);
        localCommitteeRepository.save(localCommitteeEntity);
    }

    private void updateCommitteeMembers(GovStateResult govStateResult, Integer currentEpoch, Long slot) {
        Committee committee = govStateResult.getCommittee();
        Map<Credential, Long> committeeColdCredentialEpoch = committee.getCommitteeColdCredentialEpoch();
        List<LocalCommitteeMemberEntity> localCommitteeMemberEntities = new ArrayList<>();
        committeeColdCredentialEpoch.forEach((credential, expiredEpoch) -> {
            LocalCommitteeMemberEntity entity = buildLocalCommitteeMemberEntity(credential, expiredEpoch.intValue(), currentEpoch, slot);
            localCommitteeMemberEntities.add(entity);
        });
        localCommitteeMemberRepository.saveAll(localCommitteeMemberEntities);
    }

    private void processTreasuryWithdrawals(List<GovActionId> enactedGovActionIds, Integer currentEpoch, Long slot) {
        List<GovActionProposalId> govActionProposalIds = new ArrayList<>();

        enactedGovActionIds.forEach(govActionId -> {
            GovActionProposalId govActionProposalId = new GovActionProposalId();
            govActionProposalId.setIndex(govActionId.getGov_action_index());
            govActionProposalId.setTxHash(govActionId.getTransactionId());

            govActionProposalIds.add(govActionProposalId);
        });

        List<GovActionProposalEntity> enactedTreasuryWithdrawalProposals = govActionProposalRepository.findAllById(govActionProposalIds)
                .stream()
                .filter(govActionProposalEntity -> govActionProposalEntity.getType() == GovActionType.TREASURY_WITHDRAWALS_ACTION)
                .toList();

        localTreasuryWithdrawalRepository.saveAll(
                enactedTreasuryWithdrawalProposals.stream()
                        .map(govActionProposalEntity -> {
                            JsonNode actionDetail = govActionProposalEntity.getDetails();
                            JsonNode withdrawalsNode = actionDetail.get("withdrawals");
                            Map<String, BigInteger> withdrawals = new HashMap<>();
                            withdrawalsNode.fields().forEachRemaining(entry -> {
                                String addressHash = entry.getKey();
                                BigInteger amount = entry.getValue().bigIntegerValue();
                                Address address = new Address(HexUtil.decodeHexString(addressHash));
                                withdrawals.put(address.getAddress(), amount);
                            });

                            return LocalTreasuryWithdrawalEntity.builder()
                                    .govActionTxHash(govActionProposalEntity.getTxHash())
                                    .govActionIndex((int) govActionProposalEntity.getIndex())
                                    .withdrawals(new ObjectMapper().valueToTree(withdrawals))
                                    .epoch(currentEpoch)
                                    .slot(slot)
                                    .build();
                        })
                        .toList()
        );
    }

    public Mono<GovStateResult> getGovStateFromNode() {
        try {
            localStateQueryClient.release().block(Duration.ofSeconds(5));
        } catch (Exception e) {
            // Ignore the error
        }

        try {
            localStateQueryClient.acquire().block(Duration.ofSeconds(5));
        } catch (Exception e) {
            // Ignore the error
        }

        return localStateQueryClient.executeQuery(new GovStateQuery(Era.Conway));
    }

    private LocalGovActionProposalStatusEntity buildLocalGovActionProposalEntity(GovActionId govActionId, GovActionStatus status, Integer epoch, Long slot) {
        return LocalGovActionProposalStatusEntity.builder()
                .govActionTxHash(govActionId.getTransactionId())
                .govActionIndex(govActionId.getGov_action_index())
                .epoch(epoch)
                .slot(slot)
                .status(status)
                .build();
    }

    private LocalConstitutionEntity buildLocalConstitutionEntity(Constitution constitution, Integer epoch, Long slot) {
        return LocalConstitutionEntity.builder()
                .anchorHash(constitution.getAnchor().getAnchor_data_hash())
                .anchorUrl(constitution.getAnchor().getAnchor_url())
                .epoch(epoch)
                .slot(slot)
                .script(constitution.getScripthash())
                .build();
    }

    private LocalCommitteeMemberEntity buildLocalCommitteeMemberEntity(Credential credential, Integer expiredEpoch, Integer currentEpoch, Long slot) {
        return LocalCommitteeMemberEntity.builder()
                .hash(credential.getHash())
                .credType(credential.getType() == StakeCredType.ADDR_KEYHASH ? CredentialType.ADDR_KEYHASH : CredentialType.SCRIPTHASH)
                .expiredEpoch(expiredEpoch)
                .epoch(currentEpoch)
                .slot(slot)
                .build();
    }

    private LocalCommitteeEntity buildLocalCommitteeEntity(Committee committee, Integer currentEpoch, Long slot) {
        return LocalCommitteeEntity.builder()
                .threshold(committee.getThreshold().doubleValue())
                .epoch(currentEpoch)
                .slot(slot)
                .build();
    }
}