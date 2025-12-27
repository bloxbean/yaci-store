package com.bloxbean.cardano.yaci.store.governancerules.voting;

import com.bloxbean.cardano.yaci.core.model.governance.actions.GovAction;
import com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommittee;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VotingEvaluationContext {
    GovAction govAction;
    ConstitutionCommittee committee;
    DrepVoteThresholds drepThresholds;
    PoolVotingThresholds poolThresholds;
    boolean isInBootstrapPhase;
}