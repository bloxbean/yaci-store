package com.bloxbean.cardano.yaci.store.governanceaggr.client;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("proposalStateClient")
@RequiredArgsConstructor
public class ProposalStateClientImpl implements ProposalStateClient {
    private final GovActionProposalStatusStorage govActionProposalStatusStorage;
    private final GovActionProposalStorage govActionProposalStorage;

    @Override
    public List<GovActionProposal> getActiveProposals(int epoch) {
        var proposalStatusList = govActionProposalStatusStorage.findByStatusAndEpoch(GovActionStatus.ACTIVE, epoch);

        return govActionProposalStorage.findByGovActionIds(proposalStatusList.stream()
                        .map(proposalStatus -> new GovActionId(proposalStatus.getGovActionTxHash(), proposalStatus.getGovActionIndex()))
                        .toList())
                .stream()
                .map(govActionProposal ->
                        GovActionProposal.builder()
                                .txHash(govActionProposal.getTxHash())
                                .index((int) govActionProposal.getIndex())
                                .anchorUrl(govActionProposal.getAnchorUrl())
                                .anchorHash(govActionProposal.getAnchorHash())
                                .deposit(govActionProposal.getDeposit())
                                .returnAddress(govActionProposal.getReturnAddress())
                                .type(govActionProposal.getType())
                                .blockNumber(govActionProposal.getBlockNumber())
                                .slot(govActionProposal.getSlot())
                                .epoch(govActionProposal.getEpoch())
                                .details(govActionProposal.getDetails())
                                .build())
                .collect(Collectors.toList());
    }
}
