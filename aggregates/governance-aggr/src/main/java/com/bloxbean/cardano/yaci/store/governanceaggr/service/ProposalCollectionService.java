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
import com.bloxbean.cardano.yaci.store.governancerules.domain.Proposal;
import com.bloxbean.cardano.yaci.store.governancerules.service.ProposalDropService;
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

    public List<GovActionProposal> getProposalsForStatusEvaluation(int epoch) {
        List<GovActionProposal> expiredProposalsInPrevSnapshot = proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.EXPIRED, epoch - 1);
        List<GovActionProposal> ratifiedProposalsInPrevSnapshot = proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, epoch - 1);

        List<GovActionProposal> activeProposalsInPrevEpoch =  getActiveProposalsInEpoch(epoch - 1);

        List<Proposal> activeProposals = activeProposalsInPrevEpoch
                .stream()
                .map(proposalMapper::toProposalInGovRule)
                .toList();

        List<Proposal> expiredProposals = expiredProposalsInPrevSnapshot
                .stream()
                .map(proposalMapper::toProposalInGovRule)
                .toList();

        List<Proposal> ratifiedProposals = ratifiedProposalsInPrevSnapshot
                .stream()
                .map(proposalMapper::toProposalInGovRule)
                .toList();

        ProposalDropService proposalDropService = new ProposalDropService();

        var scheduledToDropProposals =
                proposalDropService.getProposalsBeDropped(activeProposals, expiredProposals, ratifiedProposals)
                        .stream()
                        .map(Proposal::getGovActionId)
                        .toList();

        return activeProposalsInPrevEpoch
                .stream()
                .filter(govActionProposal -> !scheduledToDropProposals.contains(
                        GovActionId.builder()
                                .gov_action_index(govActionProposal.getIndex())
                                .transactionId(govActionProposal.getTxHash())
                                .build()))
                .toList();
    }

    private List<GovActionProposal> getActiveProposalsInEpoch(int epoch) {
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