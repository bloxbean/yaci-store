package com.bloxbean.cardano.yaci.store.governanceaggr.client;

import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.*;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governance.jackson.CredentialDeserializer;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.GovActionProposalStatusMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component("proposalStateClient")
@Slf4j
public class ProposalStateClientImpl implements ProposalStateClient {
    private final GovActionProposalStatusStorage govActionProposalStatusStorage;
    private final GovActionProposalStorage govActionProposalStorage;
    private final ObjectMapper objectMapper;

    public ProposalStateClientImpl(GovActionProposalStatusStorage govActionProposalStatusStorage, GovActionProposalStorage govActionProposalStorage, GovActionProposalStatusMapper govActionProposalStatusMapper) {
        this.govActionProposalStatusStorage = govActionProposalStatusStorage;
        this.govActionProposalStorage = govActionProposalStorage;

        this.objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(Credential.class, new CredentialDeserializer());
        this.objectMapper.registerModule(module);
    }

    @Override
    public List<GovActionProposal> getProposalsByStatusAndEpoch(GovActionStatus status, int epoch) {
        var proposalStatusList = govActionProposalStatusStorage.findByStatusAndEpoch(status, epoch);

        return getGovActionProposals(proposalStatusList);
    }

    @Override
    public List<GovActionProposal> getProposalsByStatusListAndEpoch(List<GovActionStatus> statusList, int epoch) {
        var proposalStatusList = govActionProposalStatusStorage.findByStatusListAndEpoch(statusList, epoch);

        return getGovActionProposals(proposalStatusList);
    }

    private List<GovActionProposal> getGovActionProposals(List<GovActionProposalStatus> proposalStatusList) {
        return govActionProposalStorage.findByGovActionIds(proposalStatusList.stream()
                        .map(proposalStatus -> new GovActionId(proposalStatus.getGovActionTxHash(), proposalStatus.getGovActionIndex()))
                        .toList())
                .stream()
                .map(govActionProposal -> {
                    var govActionDetailOpt = getGovAction(govActionProposal);

                    return govActionDetailOpt.map(govAction ->  GovActionProposal.builder()
                            .txHash(govActionProposal.getTxHash())
                            .index((int) govActionProposal.getIndex())
                            .anchorUrl(govActionProposal.getAnchorUrl())
                            .anchorHash(govActionProposal.getAnchorHash())
                            .deposit(govActionProposal.getDeposit())
                            .returnAddress(govActionProposal.getReturnAddress())
                            .blockNumber(govActionProposal.getBlockNumber())
                            .blockTime(govActionProposal.getBlockTime())
                            .slot(govActionProposal.getSlot())
                            .epoch(govActionProposal.getEpoch())
                            .govAction(govAction)
                            .build());
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<GovActionProposal> getLastEnactedProposal(GovActionType govActionType, int currentEpoch) {
        var govActionProposalStatusOpt = govActionProposalStatusStorage.findLastEnactedProposal(govActionType, currentEpoch);
        if (govActionProposalStatusOpt.isEmpty()) {
            return Optional.empty();
        }

        var govActionProposals = govActionProposalStorage.findByGovActionIds(
                List.of(new GovActionId(govActionProposalStatusOpt.get().getGovActionTxHash(), govActionProposalStatusOpt.get().getGovActionIndex())));

        if (govActionProposals.isEmpty()) {
            return Optional.empty();
        } else {
            var govActionProposal = govActionProposals.get(0);

            var govActionOpt = getGovAction(govActionProposal);
            return govActionOpt.map(govAction -> GovActionProposal.builder()
                    .txHash(govActionProposal.getTxHash())
                    .index((int) govActionProposal.getIndex())
                    .anchorUrl(govActionProposal.getAnchorUrl())
                    .anchorHash(govActionProposal.getAnchorHash())
                    .deposit(govActionProposal.getDeposit())
                    .returnAddress(govActionProposal.getReturnAddress())
                    .blockNumber(govActionProposal.getBlockNumber())
                    .blockTime(govActionProposal.getBlockTime())
                    .slot(govActionProposal.getSlot())
                    .epoch(govActionProposal.getEpoch())
                    .govAction(govAction)
                    .build());
        }
    }

    @Override
    public Optional<Integer> getLatestEpochWithStatusBefore(List<GovActionStatus> statusList, int epoch) {
        return govActionProposalStatusStorage.findLatestEpochWithStatusBefore(statusList, epoch);
    }

    private Optional<GovAction> getGovAction(com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal govActionProposal) {
        GovAction govAction = null;

        try {
            switch (govActionProposal.getType()) {
                case INFO_ACTION -> govAction = new InfoAction();
                case HARD_FORK_INITIATION_ACTION ->
                        govAction = objectMapper.treeToValue(govActionProposal.getDetails(), HardForkInitiationAction.class);
                case TREASURY_WITHDRAWALS_ACTION ->
                      govAction = objectMapper.treeToValue(govActionProposal.getDetails(), TreasuryWithdrawalsAction.class);
                case NO_CONFIDENCE -> govAction = objectMapper.treeToValue(govActionProposal.getDetails(), NoConfidence.class);
                case UPDATE_COMMITTEE ->
                        govAction = objectMapper.treeToValue(govActionProposal.getDetails(), UpdateCommittee.class);
                case NEW_CONSTITUTION ->
                        govAction = objectMapper.treeToValue(govActionProposal.getDetails(), NewConstitution.class);
                case PARAMETER_CHANGE_ACTION ->
                        govAction = objectMapper.treeToValue(govActionProposal.getDetails(), ParameterChangeAction.class);
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON for gov action detail, GovActionID: tx hash {} , index {}",
                    govActionProposal.getTxHash(), govActionProposal.getIndex(), e);
        }

        return Optional.ofNullable(govAction);
    }
}
