package com.bloxbean.cardano.yaci.store.governancerules.rule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class CommitteeVotesState extends VotesState {
    private Integer ccQuorum;
    private Integer yesVote;

    @Override
    public boolean isAccepted() {
        return yesVote >= ccQuorum;
    }

}
