package com.bloxbean.cardano.yaci.store.governancerules.evaluator.impl;

import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.RatificationEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;

/**
 * Evaluator for evaluating Info Action governance actions.
 * Info actions are always rejected as they cannot be ratified.
 */
public class InfoActionRatificationEvaluator implements RatificationEvaluator {
    
    @Override
    public RatificationResult evaluate(RatificationContext context) {
        // Info actions cannot be ratified or enacted
        return RatificationResult.REJECT;
    }
}
