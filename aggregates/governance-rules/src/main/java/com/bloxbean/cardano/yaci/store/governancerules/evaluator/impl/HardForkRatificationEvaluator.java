package com.bloxbean.cardano.yaci.store.governancerules.evaluator.impl;

import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;

/**
 * Evaluator for evaluating Hard Fork Initiation governance actions.
 */
public class HardForkRatificationEvaluator implements RatificationEvaluator {
    
    @Override
    public RatificationResult evaluate(RatificationContext context) {
        // TODO: Implement Hard Fork evaluation logic
        return RatificationResult.CONTINUE;
    }
}
