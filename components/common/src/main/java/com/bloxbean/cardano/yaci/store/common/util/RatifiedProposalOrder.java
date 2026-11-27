package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class RatifiedProposalOrder {
    private RatifiedProposalOrder() {
    }

    /** Order parameter changes from one ratification epoch by their parent links. */
    public static List<GovActionProposal> orderParameterChanges(List<GovActionProposal> proposals) {
        var changes = proposals.stream()
                .filter(proposal -> proposal.getGovAction() instanceof ParameterChangeAction)
                .toList();
        if (changes.size() < 2)
            return List.copyOf(proposals);

        var byId = new HashMap<GovActionId, GovActionProposal>();
        var byParent = new HashMap<GovActionId, GovActionProposal>();
        for (var proposal : changes) {
            var id = new GovActionId(proposal.getTxHash(), proposal.getIndex());
            var parent = ((ParameterChangeAction) proposal.getGovAction()).getGovActionId();
            if (byId.put(id, proposal) != null || byParent.put(parent, proposal) != null)
                throw invalidChain();
        }

        // The first action's parent was enacted in an earlier epoch (or is absent).
        var roots = changes.stream()
                .filter(proposal -> !byId.containsKey(((ParameterChangeAction) proposal.getGovAction()).getGovActionId()))
                .toList();
        if (roots.size() != 1)
            throw invalidChain();

        var ordered = new ArrayList<GovActionProposal>();
        var next = roots.get(0);
        while (next != null && ordered.size() < changes.size()) {
            ordered.add(next);
            next = byParent.get(new GovActionId(next.getTxHash(), next.getIndex()));
        }
        if (next != null || ordered.size() != changes.size())
            throw invalidChain();

        // Other action types retain their positions. Other linked action types delay
        // ratification, so only parameter changes can form a chain within one epoch.
        var iterator = ordered.iterator();
        return proposals.stream()
                .map(proposal -> proposal.getGovAction() instanceof ParameterChangeAction ? iterator.next() : proposal)
                .toList();
    }

    private static IllegalStateException invalidChain() {
        return new IllegalStateException("Ratified parameter changes do not form a single chain. Recompute affected governance state.");
    }
}
