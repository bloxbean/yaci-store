package com.bloxbean.cardano.yaci.store.governanceaggr.client;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.util.RatifiedProposalOrder;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.ProposalMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component("proposalStateClient")
@Primary
@Slf4j
public class ProposalStateClientImpl implements ProposalStateClient {
    private final GovActionProposalStatusStorage govActionProposalStatusStorage;
    private final GovActionProposalStorage govActionProposalStorage;
    private final ProposalMapper proposalMapper;

    public ProposalStateClientImpl(GovActionProposalStatusStorage govActionProposalStatusStorage,
                                   GovActionProposalStorage govActionProposalStorage,
                                   ProposalMapper proposalMapper) {
        this.govActionProposalStatusStorage = govActionProposalStatusStorage;
        this.govActionProposalStorage = govActionProposalStorage;
        this.proposalMapper = proposalMapper;
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
                .map(proposalMapper::toGovActionProposal)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<GovActionProposal> getLastEnactedProposal(GovActionType govActionType, int currentEpoch) {
        var statuses = govActionProposalStatusStorage.findLastEnactedProposals(govActionType, currentEpoch);
        if (statuses.isEmpty())
            return Optional.empty();

        var found = getGovActionProposals(statuses);
        var foundIds = found.stream().map(proposal -> new GovActionId(proposal.getTxHash(), proposal.getIndex()))
                .collect(Collectors.toSet());
        var missingIds = statuses.stream()
                .map(status -> new GovActionId(status.getGovActionTxHash(), status.getGovActionIndex()))
                .filter(id -> !foundIds.contains(id))
                .toList();
        // A missing link would make its parent look like the chain tip.
        if (!missingIds.isEmpty())
            throw new IllegalStateException("Ratified proposals not found or unreadable: " + missingIds);

        var proposals = RatifiedProposalOrder.orderParameterChanges(found);
        return proposals.isEmpty() ? Optional.empty() : Optional.of(proposals.getLast());
    }
}
