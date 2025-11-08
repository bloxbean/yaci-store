package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.*;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.jackson.CredentialDeserializer;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.Proposal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class ProposalMapper {
    private final ObjectMapper objectMapper;

    public ProposalMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(Credential.class, new CredentialDeserializer());
        this.objectMapper.registerModule(module);
    }

    // TODO: remove this method after refactoring
    public Proposal toProposal(GovActionProposal govActionProposal) {
        return Proposal.builder()
                .type(govActionProposal.getGovAction().getType())
                .govActionId(GovActionId.builder()
                        .gov_action_index(govActionProposal.getIndex())
                        .transactionId(govActionProposal.getTxHash())
                        .build())
                .previousGovActionId(getPrevGovActionId(govActionProposal))
                .build();
    }

    public com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal toProposalInGovRule(GovActionProposal govActionProposal) {
        return com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal.builder()
                .type(govActionProposal.getGovAction().getType())
                .govActionId(GovActionId.builder()
                        .gov_action_index(govActionProposal.getIndex())
                        .transactionId(govActionProposal.getTxHash())
                        .build())
                .prevGovActionId(getPrevGovActionId(govActionProposal))
                .build();
    }

    public Optional<GovActionProposal> toGovActionProposal(com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal govActionProposal) {
        var govActionDetailOpt = getGovAction(govActionProposal);

        if (govActionDetailOpt.isEmpty()) {
            log.error("GovAction detail missing or failed to deserialize for proposal: txHash={}, index={}",
                    govActionProposal.getTxHash(), govActionProposal.getIndex());
            return Optional.empty();
        }

        var govAction = govActionDetailOpt.get();

        return Optional.of(GovActionProposal.builder()
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

    private GovActionId getPrevGovActionId(GovActionProposal govActionProposal) {
        GovAction govAction = govActionProposal.getGovAction();

        return switch (govAction.getType()) {
            case TREASURY_WITHDRAWALS_ACTION, INFO_ACTION -> null;
            case HARD_FORK_INITIATION_ACTION -> ((HardForkInitiationAction) govAction).getGovActionId();
            case NO_CONFIDENCE -> ((NoConfidence) govAction).getGovActionId();
            case UPDATE_COMMITTEE -> ((UpdateCommittee) govAction).getGovActionId();
            case NEW_CONSTITUTION -> ((NewConstitution) govAction).getGovActionId();
            case PARAMETER_CHANGE_ACTION -> ((ParameterChangeAction) govAction).getGovActionId();
        };
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
                case NO_CONFIDENCE ->
                        govAction = objectMapper.treeToValue(govActionProposal.getDetails(), NoConfidence.class);
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
