package com.bloxbean.cardano.yaci.store.governancerules.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal;
import com.bloxbean.cardano.yaci.store.governancerules.util.ProposalUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProposalDropService {

    public List<Proposal> getProposalsBeDropped(
            List<Proposal> proposals,
            List<Proposal> expiredProposals,
            List<Proposal> ratifiedProposals) {

        Map<GovActionId, Proposal> proposalsBeDropped = new HashMap<>();

        List<Proposal> proposalsExcludingExpiredOrRatified = proposals.stream()
                .filter(p -> !expiredProposals.contains(p) && !ratifiedProposals.contains(p))
                .toList();

        expiredProposals.forEach(proposal ->
                ProposalUtils.findDescendants(proposal, proposalsExcludingExpiredOrRatified)
                        .forEach(p -> proposalsBeDropped.putIfAbsent(p.getGovActionId(), p))
        );

        ratifiedProposals.forEach(proposal ->
                ProposalUtils.findSiblingsAndTheirDescendants(proposal, proposalsExcludingExpiredOrRatified)
                        .forEach(p -> proposalsBeDropped.putIfAbsent(p.getGovActionId(), p))
        );

        return proposalsBeDropped.values().stream().toList();
    }
}
