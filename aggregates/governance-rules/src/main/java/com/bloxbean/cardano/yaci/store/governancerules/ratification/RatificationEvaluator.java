package com.bloxbean.cardano.yaci.store.governancerules.ratification;

import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;

/**
 * Evaluator interface for evaluating different types of governance actions.
 * Each governance action type has its own implementation with specific evaluation logic.
 */
public interface RatificationEvaluator {

    /**
     * Evaluates the ratification for a specific governance action type.
     *
     * @param context The ratification context containing all necessary data
     * @return The ratification result (ACCEPT, REJECT, or CONTINUE)
     * @throws IllegalArgumentException if required data for this action type is missing
     */
    RatificationResult evaluate(RatificationContext context);

    /**
     * Validates that all required data for this specific action type is present.
     *
     * @param context The ratification context to validate
     * @throws IllegalArgumentException if required data is missing
     */
    default void validateRequiredData(RatificationContext context) {
        //TODO
    }
}
