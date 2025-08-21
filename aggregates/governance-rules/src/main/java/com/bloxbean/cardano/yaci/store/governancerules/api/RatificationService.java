package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.evaluator.RatificationEvaluatorFactory;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationContext;

public class RatificationService {

    /**
     * Evaluates a governance action for ratification.
     *
     * @param input The governance ratification input containing all necessary data
     * @return The ratification result (ACCEPT, REJECT, or CONTINUE)
     */
    public RatificationResult evaluate(GovernanceRatificationInput input) {
        input.validate();

        RatificationContext context = RatificationContext.builder()
            .govAction(input.getGovAction())
            .votingData(input.getVotingData())
            .governanceState(input.getGovernanceState())
            .proposalContext(input.getProposalContext())
            .build();

        return RatificationEvaluatorFactory
            .getEvaluator(input.getGovAction().getType())
            .evaluate(context);
    }

    public static GovernanceRatificationInputBuilder builder() {
        return new GovernanceRatificationInputBuilder();
    }
}
