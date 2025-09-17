package com.bloxbean.cardano.yaci.store.governancerules.voting.committee;

import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VoteTallyCalculator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingStatus;

import java.math.BigDecimal;

public class CommitteeVotingEvaluator implements VotingEvaluator<VotingData> {

    @Override
    public VotingStatus evaluate(VotingData votingData, VotingEvaluationContext context) {
        var committee = context.getCommittee();
        var votes = votingData.getCommitteeVotes();
        
        if (committee == null || votes == null) {
            return VotingStatus.INSUFFICIENT_DATA;
        }
        
        var threshold = committee.getThreshold().safeRatio();
        if (threshold.equals(BigDecimal.ZERO)) {
            return VotingStatus.PASSED_THRESHOLD;
        }

        var committeeVoteTallies = VoteTallyCalculator.computeCommitteeTallies(votes.getVotes(), committee.getMembers());

        int yesVotes = committeeVoteTallies.getYesCount();
        int noVotes = committeeVoteTallies.getNoCount() + committeeVoteTallies.getDoNotVoteCount();

        int totalVotes = yesVotes + noVotes;
        if (totalVotes == 0) {
            return VotingStatus.NOT_PASSED_THRESHOLD;
        }
        
        BigDecimal yesRatio = BigDecimal.valueOf(yesVotes)
                .divide(BigDecimal.valueOf(totalVotes), 2, BigDecimal.ROUND_HALF_UP);

        return yesRatio.compareTo(threshold) >= 0 ?
                VotingStatus.PASSED_THRESHOLD : VotingStatus.NOT_PASSED_THRESHOLD;
    }
}
