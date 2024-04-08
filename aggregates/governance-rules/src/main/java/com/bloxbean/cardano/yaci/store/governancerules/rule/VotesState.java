package com.bloxbean.cardano.yaci.store.governancerules.rule;


import com.bloxbean.cardano.yaci.core.model.governance.actions.GovAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public abstract class VotesState {
    protected GovAction govAction;

    abstract boolean isAccepted();
}
