package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.GovAction;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProposalContext {

    GovAction govAction;
    VotingData votingData;
    GovActionId govActionId;
    Integer maxAllowedVotingEpoch;
    Long proposalSlot;

    public GovActionId getPreviousGovActionId() {
        if (govAction == null) {
            return null;
        }

        return switch (govAction.getType()) {
            case NO_CONFIDENCE -> {
                var noConfidence = (com.bloxbean.cardano.yaci.core.model.governance.actions.NoConfidence) govAction;
                yield noConfidence.getGovActionId();
            }
            case UPDATE_COMMITTEE -> {
                var updateCommittee = (com.bloxbean.cardano.yaci.core.model.governance.actions.UpdateCommittee) govAction;
                yield updateCommittee.getGovActionId();
            }
            case NEW_CONSTITUTION -> {
                var newConstitution = (com.bloxbean.cardano.yaci.core.model.governance.actions.NewConstitution) govAction;
                yield newConstitution.getGovActionId();
            }
            case HARD_FORK_INITIATION_ACTION -> {
                var hardFork = (com.bloxbean.cardano.yaci.core.model.governance.actions.HardForkInitiationAction) govAction;
                yield hardFork.getGovActionId();
            }
            case PARAMETER_CHANGE_ACTION -> {
                var paramChange = (com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction) govAction;
                yield paramChange.getGovActionId();
            }
            case TREASURY_WITHDRAWALS_ACTION, INFO_ACTION -> null;
        };
    }

    /**
     * Validates the proposal context.
     *
     * @throws IllegalArgumentException if required data is missing
     */
    public void validate() {
        if (govAction == null) {
            throw new IllegalArgumentException("ProposalContext: Governance action is required");
        }

        if (votingData == null) {
            throw new IllegalArgumentException("ProposalContext: Voting data is required");
        }

        if (govActionId == null) {
            throw new IllegalArgumentException("ProposalContext: Governance action id is required");
        }

        if (proposalSlot == null) {
            throw new IllegalArgumentException("ProposalContext: Proposal slot is required");
        }

        if (maxAllowedVotingEpoch == null) {
            throw new IllegalArgumentException("ProposalContext: Max allowed voting epoch is required");
        }

        // Validate voting data
        votingData.validate();
    }

}