package com.bloxbean.cardano.yaci.store.governancerules.api;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.actions.GovAction;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Fluent builder for creating GovernanceRatificationInput objects.
 * Provides type-safe methods for setting different types of parameters.
 */
public class GovernanceRatificationInputBuilder {
    
    private GovAction govAction;
    private VotingData.VotingDataBuilder votingDataBuilder = VotingData.builder();
    private GovernanceState.GovernanceStateBuilder governanceStateBuilder = GovernanceState.builder();
    private ProposalContext.ProposalContextBuilder proposalContextBuilder = ProposalContext.builder();
    
    // Core action setup
    public GovernanceRatificationInputBuilder govAction(GovAction govAction) {
        this.govAction = govAction;
        return this;
    }
    
    // DRep voting methods
    public GovernanceRatificationInputBuilder drepVotes(BigInteger yesStake, BigInteger noStake) {
        votingDataBuilder.drepVotes(VotingData.DRepVotes.builder()
            .yesVoteStake(yesStake)
            .noVoteStake(noStake)
            .build());
        return this;
    }
    
    // SPO voting methods
    public GovernanceRatificationInputBuilder spoVotes(BigInteger yesStake, BigInteger abstainStake, BigInteger totalStake) {
        votingDataBuilder.spoVotes(VotingData.SPOVotes.builder()
            .yesVoteStake(yesStake)
            .abstainVoteStake(abstainStake)
            .totalStake(totalStake)
            .build());
        return this;
    }
    
    // Committee voting methods
    public GovernanceRatificationInputBuilder committeeVotes(Integer yesVote, Integer noVote, BigDecimal threshold) {
        votingDataBuilder.committeeVotes(VotingData.CommitteeVotes.builder()
            .yesVote(yesVote)
            .noVote(noVote)
            .threshold(threshold)
            .build());
        return this;
    }
    
    // Governance state methods
    public GovernanceRatificationInputBuilder currentEpoch(int epoch) {
        governanceStateBuilder.currentEpoch(epoch);
        return this;
    }
    
    public GovernanceRatificationInputBuilder maxVotingEpoch(Integer maxEpoch) {
        proposalContextBuilder.maxAllowedVotingEpoch(maxEpoch);
        return this;
    }
    
    public GovernanceRatificationInputBuilder epochParam(EpochParam epochParam) {
        governanceStateBuilder.epochParam(epochParam);
        return this;
    }
    
    public GovernanceRatificationInputBuilder committeeState(ConstitutionCommitteeState state) {
        governanceStateBuilder.committeeState(state);
        return this;
    }
    
    public GovernanceRatificationInputBuilder bootstrapPhase(boolean isBootstrap) {
        governanceStateBuilder.isBootstrapPhase(isBootstrap);
        return this;
    }
    
    public GovernanceRatificationInputBuilder ratificationDelayed(boolean isDelayed) {
        governanceStateBuilder.isActionRatificationDelayed(isDelayed);
        return this;
    }
    
    public GovernanceRatificationInputBuilder lastEnactedAction(GovActionId actionId) {
        proposalContextBuilder.lastEnactedGovActionId(actionId);
        return this;
    }
    
    public GovernanceRatificationInputBuilder treasury(BigInteger treasuryAmount) {
        governanceStateBuilder.treasury(treasuryAmount);
        return this;
    }
    
    // Build method - returns input object
    public GovernanceRatificationInput build() {
        GovernanceRatificationInput input = GovernanceRatificationInput.builder()
            .govAction(govAction)
            .votingData(votingDataBuilder.build())
            .governanceState(governanceStateBuilder.build())
            .proposalContext(proposalContextBuilder.build())
            .build();

        input.validate();
        return input;
    }

    public RatificationResult evaluate() {
        GovernanceRatificationInput input = build();
        RatificationService service = new RatificationService();
        return service.evaluate(input);
    }
}
