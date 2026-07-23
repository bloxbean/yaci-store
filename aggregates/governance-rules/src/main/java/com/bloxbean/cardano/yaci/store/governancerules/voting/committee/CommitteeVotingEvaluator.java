package com.bloxbean.cardano.yaci.store.governancerules.voting.committee;

import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VoteTallyCalculator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluator;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingStatus;

import java.math.BigDecimal;
import java.math.BigInteger;

public class CommitteeVotingEvaluator implements VotingEvaluator<VotingData> {

    @Override
    public VotingStatus evaluate(VotingData votingData, VotingEvaluationContext context) {
        var committee = context.getCommittee();
        var votes = votingData.getCommitteeVotes();
        
        if (committee == null || votes == null) {
            return VotingStatus.INSUFFICIENT_DATA;
        }

        // When committee is in no confidence state (NO_CONFIDENCE), committee vote fails.
        if (ConstitutionCommitteeState.NO_CONFIDENCE.equals(committee.getState())) {
            return VotingStatus.NOT_PASS_THRESHOLD;
        }

        // Post-bootstrap: committee must meet minimum size requirement
        if (!context.isInBootstrapPhase()
                && context.getCommitteeMinSize() != null
                && committee.getMembers().size() < context.getCommitteeMinSize()) {
            return VotingStatus.NOT_PASS_THRESHOLD;
        }

        var threshold = committee.getThreshold();
        if (threshold.safeRatio().compareTo(BigDecimal.ZERO) == 0) {
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

        // Compare yes/total >= thresholdNumerator/thresholdDenominator exactly by cross-multiplying.
        BigInteger acceptedRatioSide = BigInteger.valueOf(yesVotes)
                .multiply(threshold.getDenominator());
        BigInteger thresholdSide = BigInteger.valueOf(totalExcludingAbstain)
                .multiply(threshold.getNumerator());

        return acceptedRatioSide.compareTo(thresholdSide) >= 0 ?
                VotingStatus.PASS_THRESHOLD : VotingStatus.NOT_PASS_THRESHOLD;
    }
}
