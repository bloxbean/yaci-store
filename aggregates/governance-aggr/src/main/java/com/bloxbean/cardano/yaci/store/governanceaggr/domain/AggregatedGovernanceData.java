package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.model.ProposalType;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMemberDetails;
import lombok.Builder;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Builder
public record AggregatedGovernanceData(int currentEpoch,
                                       List<GovActionProposal> proposalsForEvaluation,
                                       Map<GovActionId, AggregatedVotingData> aggregatedVotingDataByProposal,
                                       ProtocolParams protocolParams,
                                       BigInteger committeeThresholdNumerator,
                                       BigInteger committeeThresholdDenominator,
                                       List<CommitteeMemberDetails> committeeMembers,
                                       BigInteger treasury,
                                       Map<ProposalType, GovActionId> lastEnactedGovActionIds,
                                       boolean bootstrapPhase) {
}
