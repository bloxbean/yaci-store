package com.bloxbean.cardano.yaci.store.governancerules.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.governancerules.api.ProposalContext;
import com.bloxbean.cardano.yaci.store.governancerules.api.ProposalEvaluationResult;
import com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal;

import java.util.List;

public class ProposalDropService {

    public List<Proposal> getProposalsShouldBeDropped(
            List<ProposalContext> currentProposals,
            List<ProposalEvaluationResult> currentResults) {
        // TODO: implement this
        return List.of();
    }

    private Proposal convertToProposal(ProposalContext proposalContext) {
        GovActionId govActionId = proposalContext.getGovActionId();
        GovActionId prevGovActionId = proposalContext.getPreviousGovActionId();
        
        return Proposal.builder()
            .govActionId(govActionId)
            .previousGovActionId(prevGovActionId)
            .type(proposalContext.getGovAction().getType())
            .build();
    }
}
