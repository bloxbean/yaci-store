package com.bloxbean.cardano.yaci.store.client.governance;

import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;

import java.util.List;

public interface ProposalStateClient {
    List<GovActionProposal> getProposals(GovActionStatus status, int epoch);
}
