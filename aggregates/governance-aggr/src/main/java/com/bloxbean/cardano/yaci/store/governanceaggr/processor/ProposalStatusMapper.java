package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.ProposalVotingStats;
import com.bloxbean.cardano.yaci.store.governancerules.api.GovernanceEvaluationResult;
import com.bloxbean.cardano.yaci.store.governancerules.api.ProposalEvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProposalStatusMapper {
    
    public List<GovActionProposalStatus> mapToProposalStatus(GovernanceEvaluationResult result, int currentEpoch) {
        return result.getProposalResults().stream()
            .map(proposalResult -> mapSingleProposal(proposalResult, currentEpoch))
            .toList();
    }
    
    private GovActionProposalStatus mapSingleProposal(ProposalEvaluationResult proposalResult, int currentEpoch) {
        var proposal = proposalResult.getProposal();
        
        GovActionStatus status = switch (proposalResult.getStatus()) {
            case ACCEPT -> GovActionStatus.RATIFIED;
            case REJECT -> GovActionStatus.EXPIRED;
            case CONTINUE -> GovActionStatus.ACTIVE;
        };
        
        ProposalVotingStats votingStats = createVotingStats();
        
        return GovActionProposalStatus.builder()
            .govActionTxHash(proposal.getGovActionId().getTransactionId())
            .govActionIndex(proposal.getGovActionId().getGov_action_index())
            .type(proposal.getType())
            .status(status)
            .votingStats(votingStats)
            .epoch(currentEpoch)
            .build();
    }
    
    private ProposalVotingStats createVotingStats() {
        return ProposalVotingStats.builder()
            .spoTotalYesStake(BigInteger.ZERO)
            .spoTotalNoStake(BigInteger.ZERO)
            .spoTotalAbstainStake(BigInteger.ZERO)
            .drepTotalYesStake(BigInteger.ZERO)
            .drepTotalNoStake(BigInteger.ZERO)
            .drepNoVoteStake(BigInteger.ZERO)
            .drepNotVotedStake(BigInteger.ZERO)
            .drepNoConfidenceStake(BigInteger.ZERO)
            .drepTotalAbstainStake(BigInteger.ZERO)
            .ccYes(0)
            .ccNo(0)
            .ccDoNotVote(0)
            .ccAbstain(0)
            .build();
    }
}