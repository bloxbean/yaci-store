package com.bloxbean.cardano.yaci.store.governancerules.ratification.impl;

import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.ratification.RatificationEvaluator;
import lombok.extern.slf4j.Slf4j;

/**
 * Evaluator for evaluating Info Action governance actions.
 * Info actions cannot be ratified or enacted, since they do not have any effect on the protocol.
 */
@Slf4j
public class InfoActionRatificationEvaluator implements RatificationEvaluator {
    
    @Override
    public RatificationResult evaluate(RatificationContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Info actions cannot be ratified or enacted, since they do not have any effect on the protocol.");
        }

        if (context.getGovernanceContext().getCurrentEpoch() - context.getMaxAllowedVotingEpoch() >= 1) {
            return RatificationResult.REJECT;
        }

        return RatificationResult.CONTINUE;
    }
}
