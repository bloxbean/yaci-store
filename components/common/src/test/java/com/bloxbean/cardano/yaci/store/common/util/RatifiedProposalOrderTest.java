package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatifiedProposalOrderTest {
    @Test
    void ordersSameSlotActionsByParentLinksAndKeepsInputUnchanged() {
        var a = proposal(3, 0);
        var b = proposal(2, 3);
        var c = proposal(1, 2);
        var input = List.of(c, a, b);
        assertThat(RatifiedProposalOrder.orderParameterChanges(input)).containsExactly(a, b, c);
        assertThat(input).containsExactly(c, a, b);
    }

    @Test
    void refusesAmbiguousOrCyclicStoredChains() {
        assertThatThrownBy(() -> RatifiedProposalOrder.orderParameterChanges(List.of(proposal(1, 0), proposal(2, 0))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> RatifiedProposalOrder.orderParameterChanges(List.of(proposal(1, 2), proposal(2, 1))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> RatifiedProposalOrder.orderParameterChanges(List.of(proposal(1, 0), proposal(2, 5))))
                .isInstanceOf(IllegalStateException.class);
    }

    private GovActionProposal proposal(int id, int parent) {
        return GovActionProposal.builder().txHash("%064x".formatted(id)).index(0).slot(100L)
                .govAction(ParameterChangeAction.builder()
                        .govActionId(new GovActionId("%064x".formatted(parent), 0)).build()).build();
    }
}
