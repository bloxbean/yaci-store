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
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.core.service.TipFinderService;
import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.*;
import com.bloxbean.cardano.yaci.store.governance.storage.*;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionProposalEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionProposalId;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.GovActionProposalRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
@Slf4j
public class LocalGovStateService {
    private final LocalClientProvider localClientProvider;
    private final LocalStateQueryClient localStateQueryClient;
    private final LocalGovActionProposalStatusStorage localGovActionProposalStatusStorage;
    private final LocalGovActionProposalStatusStorageReader localGovActionProposalStatusStorageReader;
    private final LocalConstitutionStorage localConstitutionStorage;
    private final LocalCommitteeMemberStorage localCommitteeMemberStorage;
    private final LocalCommitteeStorage localCommitteeStorage;
    private final LocalHardForkInitiationStorage localHardForkInitiationStorage;
    private final GovActionProposalStorage govActionProposalStorage;
    private final LocalTreasuryWithdrawalStorage localTreasuryWithdrawalStorage;
    private final TipFinderService tipFinderService;
    private final EraService eraService;

    @Value("${store.cardano.n2c-era:Conway}")
    private String eraStr;
    private Era era;

    public LocalGovStateService(LocalClientProvider localClientProvider,
                                LocalGovActionProposalStatusStorage localGovActionProposalStatusStorage,
                                LocalGovActionProposalStatusStorageReader localGovActionProposalStatusStorageReader,
                                LocalConstitutionStorage localConstitutionStorage,
                                LocalCommitteeMemberStorage localCommitteeMemberStorage,
                                LocalCommitteeStorage localCommitteeStorage,
                                LocalHardForkInitiationStorage localHardForkInitiationStorage,
                                GovActionProposalStorage govActionProposalStorage,
                                LocalTreasuryWithdrawalStorage localTreasuryWithdrawalStorage,
                                TipFinderService tipFinderService,
                                EraService eraService) {
        this.localClientProvider = localClientProvider;
        this.localStateQueryClient = localClientProvider.getLocalStateQueryClient();
        this.localGovActionProposalStatusStorage = localGovActionProposalStatusStorage;
        this.localGovActionProposalStatusStorageReader = localGovActionProposalStatusStorageReader;
        this.localConstitutionStorage = localConstitutionStorage;
        this.localCommitteeMemberStorage = localCommitteeMemberStorage;
        this.localCommitteeStorage = localCommitteeStorage;
        this.localTreasuryWithdrawalStorage = localTreasuryWithdrawalStorage;
        this.localHardForkInitiationStorage = localHardForkInitiationStorage;
        this.govActionProposalStorage = govActionProposalStorage;
        this.tipFinderService = tipFinderService;
        this.eraService = eraService;

        log.info("LocalGovActionStateService initialized >>>");
    }

    @PostConstruct
    void init() {
        if (StringUtil.isEmpty(eraStr))
            eraStr = "Conway";

        era = Era.valueOf(eraStr);
    }

    @EventListener
    public void blockEvent(BlockHeaderEvent blockHeaderEvent) {
        if (!blockHeaderEvent.getMetadata().isSyncMode())
            return;

        if (blockHeaderEvent.getMetadata().getEra() != null
                && !blockHeaderEvent.getMetadata().getEra().name().equalsIgnoreCase(era.name())) {
            era = Era.valueOf(blockHeaderEvent.getMetadata().getEra().name());
            log.info("Gov State: Era changed to {}", era.name());
            log.info("Fetching gov state ...");
            fetchAndSetGovState();
        }
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

        List<LocalGovActionProposalStatus> govActionsInPrevEpoch = localGovActionProposalStatusStorageReader
                .findByEpochAndStatusIn(currentEpoch - 1, List.of(GovActionStatus.EXPIRED, GovActionStatus.RATIFIED));

        List<LocalGovActionProposalStatus> entitiesToSave = new ArrayList<>();

        processExpiredProposals(govStateResult, currentEpoch, slot, entitiesToSave);
        processRatifiedProposals(govStateResult, currentEpoch, slot, entitiesToSave);
        processEnactedAndDroppedProposals(govActionsInPrevEpoch, govStateResult, currentEpoch, slot, entitiesToSave);

        if (!entitiesToSave.isEmpty()) {
            localGovActionProposalStatusStorage.saveAll(entitiesToSave);
        }

        Map<ProposalType, GovActionId> lastEnactedGovActionIds = govStateResult.getNextRatifyState().getNextEnactState().getPrevGovActionIds();

        processConstitution(govStateResult, currentEpoch, slot);
        processCommittee(govStateResult, currentEpoch, slot);
        processCommitteeMembers(govStateResult, currentEpoch, slot);

        List<GovActionId> enactedGovActionIds = entitiesToSave.stream().filter(entity -> entity.getStatus().equals(GovActionStatus.ENACTED))
                .map(entity -> GovActionId.builder()
                        .transactionId(entity.getGovActionTxHash())
                        .gov_action_index((int) entity.getGovActionIndex())
                        .build()).toList();

        processTreasuryWithdrawals(enactedGovActionIds, currentEpoch, slot);
        processHardForkInitiation(enactedGovActionIds, currentEpoch, slot);
    }

    private void processExpiredProposals(GovStateResult govStateResult, Integer currentEpoch, Long slot, List<LocalGovActionProposalStatus> entitiesToSave) {
        List<GovActionId> expiredGovActions = govStateResult.getNextRatifyState().getExpiredGovActions();
        expiredGovActions.forEach(govActionId -> entitiesToSave.add(buildLocalGovActionProposal(govActionId, GovActionStatus.EXPIRED, currentEpoch, slot)));
    }

    private void processRatifiedProposals(GovStateResult govStateResult, Integer currentEpoch, Long slot, List<LocalGovActionProposalStatus> entitiesToSave) {
        List<GovActionId> ratifiedGovActions = govStateResult.getNextRatifyState().getEnactedGovActions()
                .stream()
                .map(Proposal::getGovActionId)
                .toList();
        ratifiedGovActions.forEach(govActionId -> entitiesToSave.add(buildLocalGovActionProposal(govActionId, GovActionStatus.RATIFIED, currentEpoch, slot)));
    }

    private void processEnactedAndDroppedProposals(List<LocalGovActionProposalStatus> govActionsInPrevEpoch, GovStateResult govStateResult, Integer currentEpoch, Long slot, List<LocalGovActionProposalStatus> entitiesToSave) {
        List<GovActionId> proposalsInNextRatify = govStateResult.getProposals().stream().map(Proposal::getGovActionId).toList();

        govActionsInPrevEpoch.forEach(govAction -> {
            GovActionId govActionId = GovActionId.builder()
                    .transactionId(govAction.getGovActionTxHash())
                    .gov_action_index((int) govAction.getGovActionIndex())
                    .build();

            if (govAction.getStatus().equals(GovActionStatus.RATIFIED) && !proposalsInNextRatify.contains(govActionId)) {
                entitiesToSave.add(buildLocalGovActionProposal(govActionId, GovActionStatus.ENACTED, currentEpoch, slot));
            }

            if (govAction.getStatus().equals(GovActionStatus.EXPIRED) && !proposalsInNextRatify.contains(govActionId)) {
                entitiesToSave.add(buildLocalGovActionProposal(govActionId, GovActionStatus.DROPPED, currentEpoch, slot));
            }
        });
    }

    private void processConstitution(GovStateResult govStateResult, Integer currentEpoch, Long slot) {
        Constitution constitution = govStateResult.getConstitution();
        LocalConstitution localConstitution = buildLocalConstitution(constitution, currentEpoch, slot);

        localConstitutionStorage.save(localConstitution);
    }

    private void processCommittee(GovStateResult govStateResult, Integer currentEpoch, Long slot) {
        Committee committee = govStateResult.getCommittee();
        LocalCommittee localCommittee = buildLocalCommittee(committee, currentEpoch, slot);

        localCommitteeStorage.save(localCommittee);
    }

    private void processCommitteeMembers(GovStateResult govStateResult, Integer currentEpoch, Long slot) {
        Committee committee = govStateResult.getCommittee();
        Map<Credential, Long> committeeColdCredentialEpoch = committee.getCommitteeColdCredentialEpoch();
        List<LocalCommitteeMember> localCommitteeMemberEntities = new ArrayList<>();
        committeeColdCredentialEpoch.forEach((credential, expiredEpoch) -> {
            LocalCommitteeMember entity = buildLocalCommitteeMember(credential, expiredEpoch.intValue(), currentEpoch, slot);
            localCommitteeMemberEntities.add(entity);
        });

        localCommitteeMemberStorage.saveAll(localCommitteeMemberEntities);
    }

    private void processTreasuryWithdrawals(List<GovActionId> enactedGovActionIds, Integer currentEpoch, Long slot) {
        List<GovActionProposal> enactedTreasuryWithdrawalProposals = govActionProposalStorage.findByGovActionIds(enactedGovActionIds)
                .stream()
                .filter(govActionProposal -> govActionProposal.getType() == GovActionType.TREASURY_WITHDRAWALS_ACTION)
                .toList();

        localTreasuryWithdrawalStorage.saveAll(
                enactedTreasuryWithdrawalProposals.stream()
                        .flatMap(govActionProposalEntity -> {
                            JsonNode actionDetail = govActionProposalEntity.getDetails();
                            JsonNode withdrawalsNode = actionDetail.get("withdrawals");
                            List<LocalTreasuryWithdrawal> treasuryWithdrawalEntities = new ArrayList<>();
                            withdrawalsNode.fields().forEachRemaining(entry -> {
                                String addressHash = entry.getKey();
                                BigInteger amount = entry.getValue().bigIntegerValue();
                                Address address = new Address(HexUtil.decodeHexString(addressHash));
                                var entity = LocalTreasuryWithdrawal.builder()
                                        .govActionTxHash(govActionProposalEntity.getTxHash())
                                        .govActionIndex((int) govActionProposalEntity.getIndex())
                                        .epoch(currentEpoch)
                                        .amount(amount.longValue())
                                        .address(address.getAddress())
                                        .slot(slot)
                                        .build();
                                treasuryWithdrawalEntities.add(entity);
                            });

                            return treasuryWithdrawalEntities.stream();
                        }).toList());
    }

    private void processHardForkInitiation(List<GovActionId> enactedGovActionIds, Integer currentEpoch, Long slot) {
        List<GovActionProposal> enactedHardForkInitProposals = govActionProposalStorage.findByGovActionIds(enactedGovActionIds)
                .stream()
                .filter(govActionProposalEntity -> govActionProposalEntity.getType() == GovActionType.HARD_FORK_INITIATION_ACTION)
                .toList();

        localHardForkInitiationStorage.saveAll(
                enactedHardForkInitProposals.stream()
                        .map(govActionProposal -> {
                            JsonNode actionDetail = govActionProposal.getDetails();
                            JsonNode protocolVersionNode = actionDetail.get("protocolVersion");
                            Integer major = protocolVersionNode.get("_1") != null ? protocolVersionNode.get("_1").asInt() : null;
                            Integer minor = protocolVersionNode.get("_2") != null ? protocolVersionNode.get("_2").asInt() : null;

                            return LocalHardForkInitiation.builder()
                                    .govActionTxHash(govActionProposal.getTxHash())
                                    .govActionIndex((int) govActionProposal.getIndex())
                                    .epoch(currentEpoch)
                                    .majorVersion(major)
                                    .minorVersion(minor)
                                    .slot(slot)
                                    .build();
                        }).toList());
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

        return localStateQueryClient.executeQuery(new GovStateQuery(era));
    }

    private LocalGovActionProposalStatus buildLocalGovActionProposal(GovActionId govActionId, GovActionStatus status, Integer epoch, Long slot) {
        return LocalGovActionProposalStatus.builder()
                .govActionTxHash(govActionId.getTransactionId())
                .govActionIndex(govActionId.getGov_action_index())
                .epoch(epoch)
                .slot(slot)
                .status(status)
                .build();
    }

    private LocalConstitution buildLocalConstitution(Constitution constitution, Integer epoch, Long slot) {
        return LocalConstitution.builder()
                .anchorHash(constitution.getAnchor().getAnchor_data_hash())
                .anchorUrl(constitution.getAnchor().getAnchor_url())
                .epoch(epoch)
                .slot(slot)
                .script(constitution.getScripthash())
                .build();
    }

    private LocalCommitteeMember buildLocalCommitteeMember(Credential credential, Integer expiredEpoch, Integer currentEpoch, Long slot) {
        return LocalCommitteeMember.builder()
                .hash(credential.getHash())
                .credType(credential.getType() == StakeCredType.ADDR_KEYHASH ? CredentialType.ADDR_KEYHASH : CredentialType.SCRIPTHASH)
                .expiredEpoch(expiredEpoch)
                .epoch(currentEpoch)
                .slot(slot)
                .build();
    }

    private LocalCommittee buildLocalCommittee(Committee committee, Integer currentEpoch, Long slot) {
        return LocalCommittee.builder()
                .threshold(committee.getThreshold().doubleValue())
                .epoch(currentEpoch)
                .slot(slot)
                .build();
    }
}