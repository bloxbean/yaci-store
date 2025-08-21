package com.bloxbean.cardano.yaci.store.governancerules.evaluator;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.impl.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating appropriate ratification evaluators based on governance action type.
 */
public class RatificationEvaluatorFactory {

    private static final Map<GovActionType, RatificationEvaluator> EVALUATOR_CACHE = new ConcurrentHashMap<>();

    static {
        EVALUATOR_CACHE.put(GovActionType.NO_CONFIDENCE, new NoConfidenceRatificationEvaluator());
        EVALUATOR_CACHE.put(GovActionType.UPDATE_COMMITTEE, new UpdateCommitteeRatificationEvaluator());
        EVALUATOR_CACHE.put(GovActionType.HARD_FORK_INITIATION_ACTION, new HardForkRatificationEvaluator());
        EVALUATOR_CACHE.put(GovActionType.NEW_CONSTITUTION, new NewConstitutionRatificationEvaluator());
        EVALUATOR_CACHE.put(GovActionType.TREASURY_WITHDRAWALS_ACTION, new TreasuryWithdrawalRatificationEvaluator());
        EVALUATOR_CACHE.put(GovActionType.PARAMETER_CHANGE_ACTION, new ParameterChangeRatificationEvaluator());
        EVALUATOR_CACHE.put(GovActionType.INFO_ACTION, new InfoActionRatificationEvaluator());
    }

    public static RatificationEvaluator getEvaluator(GovActionType actionType) {
        RatificationEvaluator evaluator = EVALUATOR_CACHE.get(actionType);
        if (evaluator == null) {
            throw new IllegalArgumentException("Unsupported governance action type: " + actionType);
        }
        return evaluator;
    }
}
