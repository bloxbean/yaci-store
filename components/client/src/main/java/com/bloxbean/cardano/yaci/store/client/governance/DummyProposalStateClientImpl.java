package com.bloxbean.cardano.yaci.store.client.governance;

import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;

import java.util.List;

public class DummyProposalStateClientImpl implements ProposalStateClient {
    @Override
    public List<GovActionProposal> getActiveProposals(int epoch) {
        return List.of();
    }
}
