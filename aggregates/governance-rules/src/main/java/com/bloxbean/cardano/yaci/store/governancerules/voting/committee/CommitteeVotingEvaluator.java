package com.bloxbean.cardano.yaci.store.governancerules.voting.committee;

import com.bloxbean.cardano.yaci.store.common.util.BigNumberUtils;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VoteTallyCalculator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
            return VotingStatus.PASS_THRESHOLD;
        }

        var committeeVoteTallies = VoteTallyCalculator.computeCommitteeTallies(votes.getVotes(), committee.getMembers());

        int yesVotes = committeeVoteTallies.getYesCount();

        // Do not vote is considered as No vote
        int noVotes = committeeVoteTallies.getNoCount() + committeeVoteTallies.getDoNotVoteCount();

        int totalExcludingAbstain = yesVotes + noVotes;
        if (totalExcludingAbstain == 0) {
            return VotingStatus.NOT_PASS_THRESHOLD;
        }
        
        BigDecimal acceptedRatio = BigDecimal.valueOf(yesVotes)
                .divide(BigDecimal.valueOf(totalExcludingAbstain), 4, RoundingMode.HALF_UP);

        return acceptedRatio.compareTo(threshold) >= 0 ?
                VotingStatus.PASS_THRESHOLD : VotingStatus.NOT_PASS_THRESHOLD;
    }
}
