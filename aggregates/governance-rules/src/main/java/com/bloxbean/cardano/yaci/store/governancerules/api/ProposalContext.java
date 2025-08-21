package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProposalContext {
    // Last enacted governance action of the same purpose
    GovActionId lastEnactedGovActionId;
    Integer maxAllowedVotingEpoch;
}
