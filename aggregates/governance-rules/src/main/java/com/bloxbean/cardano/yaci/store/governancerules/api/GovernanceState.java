package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.domain.EpochParam;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;

/**
 * Represents the current state of governance.
 * Contains epoch information, committee state, and protocol parameters.
 */
@Value
@Builder
public class GovernanceState {
    
    // Current epoch information
    int currentEpoch;

    // Current epoch parameters
    EpochParam epochParam;
    
    // Constitutional committee state
    ConstitutionCommitteeState committeeState;
    
    // Whether we're in Conway bootstrap phase
    boolean isBootstrapPhase;

    // Whether ratification is delayed by previous actions
    boolean isActionRatificationDelayed;

    // Current treasury amount (for treasury withdrawals)
    BigInteger treasury;

    /**
     * Checks if the committee is in normal state.
     * 
     * @return true if committee state is NORMAL
     */
    public boolean isCommitteeNormal() {
        return ConstitutionCommitteeState.NORMAL.equals(committeeState);
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
