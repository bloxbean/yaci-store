package com.bloxbean.cardano.yaci.store.test.e2e.common;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;

public interface LedgerGovernanceStateReader {
    ProposalLedgerSnapshot fetchProposalState(GovActionId govActionId);
}
