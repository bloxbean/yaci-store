package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMemberDetails;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepDist;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepDistStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.AggregatedVotingData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VotingDataCollector {
    
    private final VotingAggrService votingAggrService;
    private final CommitteeMemberStorage committeeMemberStorage;
    private final DRepDistStorageReader dRepDistStorage;
    private final BootstrapPhaseDetector bootstrapPhaseDetector;
    private final SPOVotingDataCollector spoVotingDataCollector;

    public Map<GovActionId, AggregatedVotingData> collectVotingDataBatch(List<GovActionProposal> proposals, int epoch) {
        if (proposals.isEmpty()) {
            return Collections.emptyMap();
        }
        
        boolean isInConwayBootstrapPhase = bootstrapPhaseDetector.isInConwayBootstrapPhase(epoch);
        
        List<GovActionId> govActionIds = proposals.stream()
            .map(p -> GovActionId.builder()
                .transactionId(p.getTxHash())
                .gov_action_index(p.getIndex())
                .build())
            .toList();
        
        var dRepVotes = isInConwayBootstrapPhase ? Collections.<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure>emptyList()
            : votingAggrService.getVotesByDRep(epoch, govActionIds);
        var spoVotes = votingAggrService.getVotesBySPO(epoch, govActionIds);
        
        List<CommitteeMemberDetails> membersCanVote = committeeMemberStorage.getActiveCommitteeMembersDetailsByEpoch(epoch);
        List<String> hotKeys = membersCanVote.stream().map(CommitteeMemberDetails::getHotKey).distinct().toList();
        var committeeVotes = votingAggrService.getVotesByCommittee(epoch, govActionIds, hotKeys);
        
        return proposals.stream()
            .collect(Collectors.toMap(
                p -> GovActionId.builder()
                    .transactionId(p.getTxHash())
                    .gov_action_index(p.getIndex())
                    .build(),
                p -> createVotingData(p, dRepVotes, spoVotes, committeeVotes, isInConwayBootstrapPhase, epoch)
            ));
    }
    
    private AggregatedVotingData createVotingData(GovActionProposal proposal,
                                        List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> drepVotes,
                                        List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> spoVotes,
                                        List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> committeeVotes,
                                        boolean isInConwayBootstrapPhase,
                                        int epoch) {
        return AggregatedVotingData.builder()
            .drepVotes(isInConwayBootstrapPhase ? createEmptyDRepVotes() : collectDRepVotes(proposal, drepVotes, epoch + 1))
            .spoVotes(spoVotingDataCollector.collectSPOVotes(proposal, filterVotesForProposal(spoVotes, proposal), isInConwayBootstrapPhase, epoch))
            .committeeVotes(collectCommitteeVotes(proposal, committeeVotes))
            .build();
    }
    
    private AggregatedVotingData.DRepVotes createEmptyDRepVotes() {
        return AggregatedVotingData.DRepVotes.builder()
            .yesVoteStake(BigInteger.ZERO)
            .noVoteStake(BigInteger.ZERO)
            .noConfidenceStake(BigInteger.ZERO)
            .doNotVoteStake(BigInteger.ZERO)
            .build();
    }

    private AggregatedVotingData.DRepVotes collectDRepVotes(GovActionProposal proposal,
                                                  List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> dRepVotes,
                                                  int epoch) {
        var proposalDRepVotes = filterVotesForProposal(dRepVotes, proposal);
        var noConfidenceStake = dRepDistStorage.getStakeByDRepTypeAndEpoch(DrepType.NO_CONFIDENCE, epoch).orElse(BigInteger.ZERO);
        var yesVoteStake = calculateDRepStakeByVote(proposalDRepVotes, Vote.YES, epoch);
        var noVoteStake = calculateDRepStakeByVote(proposalDRepVotes, Vote.NO, epoch);
        var abstainVoteStake = calculateDRepStakeByVote(proposalDRepVotes, Vote.ABSTAIN, epoch);
        var totalDRepStake = dRepDistStorage.getTotalStakeExcludeInactiveDRepForEpoch(epoch)
                .orElse(BigInteger.ZERO);
        var autoAbstainStake = dRepDistStorage.getStakeByDRepTypeAndEpoch(DrepType.ABSTAIN, epoch).orElse(BigInteger.ZERO);
        var doNotVoteStake = totalDRepStake.subtract(totalDRepStake).subtract(autoAbstainStake)
                .subtract(noConfidenceStake);

        return AggregatedVotingData.DRepVotes.builder()
                .yesVoteStake(yesVoteStake)
                .noVoteStake(noVoteStake)
                .abstainVoteStake(abstainVoteStake)
                .autoAbstainStake(autoAbstainStake)
                .noConfidenceStake(noConfidenceStake)
                .doNotVoteStake(doNotVoteStake)
                .build();
    }

    private AggregatedVotingData.CommitteeVotes collectCommitteeVotes(GovActionProposal proposal,
                                                            List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> committeeVotes) {
        Map<String, Vote> votes = filterVotesForProposal(committeeVotes, proposal).stream()
            .collect(Collectors.toMap(
                    VotingProcedure::getVoterHash,
                    VotingProcedure::getVote,
                (existing, replacement) -> existing
            ));
        
        return AggregatedVotingData.CommitteeVotes.builder()
            .votes(votes)
            .build();
    }
    
    private List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> filterVotesForProposal(
            List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> votes, 
            GovActionProposal proposal) {
        return votes.stream()
            .filter(v -> v.getGovActionTxHash().equals(proposal.getTxHash()) && 
                        v.getGovActionIndex() == proposal.getIndex())
            .toList();
    }
    
    private BigInteger calculateDRepStakeByVote(List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> votes, 
                                               Vote voteType, int epoch) {
        var drepIds = votes.stream()
            .filter(vote -> vote.getVote().equals(voteType))
            .map(VotingProcedure::getVoterHash)
            .toList();
        
        if (drepIds.isEmpty()) {
            return BigInteger.ZERO;
        }
        
        return dRepDistStorage.getAllByEpochAndDRepIdsExcludeInactiveDReps(epoch, drepIds)
            .stream()
            .map(DRepDist::getAmount)
            .reduce(BigInteger.ZERO, BigInteger::add);
    }

}
