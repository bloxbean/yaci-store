package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.ProposalMapper;
import com.bloxbean.cardano.yaci.store.governancerules.api.ProposalContext;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProposalCollectionService {
    
    private final GovActionProposalStorage govActionProposalStorage;
    private final ProposalStateClient proposalStateClient;
    private final EpochParamStorage epochParamStorage;
    private final ProposalMapper proposalMapper;
    
    public List<ProposalContext> createProposalContexts(List<GovActionProposal> proposals, Map<GovActionId, VotingData> votingDataMap) {
        return proposals.stream()
                .map(proposal -> mapToProposalContext(proposal, votingDataMap))
                .toList();
    }
    
    public List<GovActionProposal> getActiveProposalsInEpoch(int epoch) {
        List<GovActionProposal> newProposals = govActionProposalStorage.findByEpoch(epoch)
                .stream()
                .map(proposalMapper::toGovActionProposal)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        List<GovActionProposal> activeProposals = proposalStateClient
                .getProposalsByStatusAndEpoch(GovActionStatus.ACTIVE, epoch);

        return Stream.concat(newProposals.stream(), activeProposals.stream()).toList();
    }

    private ProposalContext mapToProposalContext(GovActionProposal proposal, Map<GovActionId, VotingData> votingDataMap) {
        GovActionId govActionId = GovActionId.builder()
                .transactionId(proposal.getTxHash())
                .gov_action_index(proposal.getIndex())
                .build();

        VotingData votingData = votingDataMap.get(govActionId);

        var govActionLifetime = epochParamStorage.getProtocolParams(proposal.getEpoch())
                .map(ep -> ep.getParams().getGovActionLifetime())
                .orElseThrow(() -> new IllegalStateException("Gov action lifetime not found for epoch: " + proposal.getEpoch()));

        int maxAllowedVotingEpoch = proposal.getEpoch() + govActionLifetime;

        return ProposalContext.builder()
                .govAction(proposal.getGovAction())
                .votingData(votingData)
                .govActionId(govActionId)
                .maxAllowedVotingEpoch(maxAllowedVotingEpoch)
                .proposalSlot(proposal.getSlot())
                .build();
    }
}