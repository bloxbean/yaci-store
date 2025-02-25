package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.*;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.Proposal;
import org.springframework.stereotype.Component;

@Component
public class ProposalMapper {

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

}
