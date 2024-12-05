package com.bloxbean.cardano.yaci.store.client.governance;

import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;

import java.util.List;

public interface ProposalStateClient {
    List<GovActionProposal> getActiveProposals(int epoch);
}
