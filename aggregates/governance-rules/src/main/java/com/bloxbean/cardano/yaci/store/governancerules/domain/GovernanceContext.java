package com.bloxbean.cardano.yaci.store.governancerules.domain;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;
import java.util.Map;

@Value
@Builder
public class GovernanceContext {
    // Current epoch information
    int currentEpoch;

    // Current protocol parameters
    ProtocolParams protocolParams;

    // Constitutional committee state
    ConstitutionCommittee committee;

    // Whether we're in Conway bootstrap phase
    boolean isInBootstrapPhase;

    // Whether ratification is delayed by previous actions
    boolean isActionRatificationDelayed;

    // Current treasury amount (for treasury withdrawals)
    BigInteger treasury;

    // Last enacted gov actions
    Map<ProposalType, GovActionId> lastEnactedGovActionIds;

    /**
     * Checks if the committee is in normal state.
     *
     * @return true if committee state is NORMAL
     */
    public boolean isCommitteeNormal() {
        return ConstitutionCommitteeState.NORMAL.equals(committee.getState());
    }

    /**
     * Checks if ratification is not delayed.
     *
     * @return true if ratification is not delayed
     */
    public boolean isNotDelayed() {
        return !isActionRatificationDelayed;
    }
}
