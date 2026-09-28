package com.bloxbean.cardano.yaci.store.test.e2e.common;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;

public record ProposalLedgerSnapshot(
        GovActionId govActionId,
        ProposalLedgerState state,
        boolean presentInCurrentProposals,
        boolean presentInEnactedGovActions,
        boolean presentInExpiredGovActions,
        boolean removedFromCurrentProposals,
        boolean ratificationDelayed,
        Integer proposedIn,
        Integer expiresAfter) {
}
