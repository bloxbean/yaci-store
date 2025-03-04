package com.bloxbean.cardano.yaci.store.governancerules.rule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class CommitteeVotingState extends VotingState {
    private BigDecimal threshold;
    private Integer yesVote;
    private Integer noVote;

    @Override
    public boolean isAccepted() {
        if (threshold == null || yesVote == null || noVote == null) {
            return false;
        }
        if (threshold.equals(BigDecimal.ZERO)) {
            return true;
        }

        int totalVotes = yesVote + noVote;
        if (totalVotes == 0) {
            return false;
        }

        BigDecimal yesVoteRatio = BigDecimal.valueOf(yesVote).divide(BigDecimal.valueOf(totalVotes), 2, BigDecimal.ROUND_HALF_UP);

        return yesVoteRatio.compareTo(threshold) >= 0;
    }

}
