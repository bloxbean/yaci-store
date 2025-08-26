package com.bloxbean.cardano.yaci.store.governancerules.evaluator.impl;

import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.RatificationEvaluator;
import lombok.extern.slf4j.Slf4j;

/**
 * Evaluator for evaluating Info Action governance actions.
 * Info actions are always rejected as they cannot be ratified.
 */
@Slf4j
public class InfoActionRatificationEvaluator implements RatificationEvaluator {
    
    @Override
    public RatificationResult evaluate(RatificationContext context) {
        log.error("Info actions cannot be ratified or enacted, since they do not have any effect on the protocol.");
        // Info actions cannot be ratified or enacted
        return RatificationResult.REJECT;
    }
}
