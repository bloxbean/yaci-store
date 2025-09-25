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
// Builds combined DRep, SPO, and committee voting data for proposals in an epoch
public class VotingDataCollector {

    private final VotingAggrService votingAggrService;
    private final CommitteeMemberStorage committeeMemberStorage;
    private final DRepDistStorageReader dRepDistStorage;
    private final BootstrapPhaseService bootstrapPhaseDetector;
    private final SPOVotingDataCollector spoVotingDataCollector;

    /**
     * Collect aggregated voting data for the provided proposals in the given epoch.
     *
     * @param proposals list of proposals to collect voting data for
     * @param epoch     snapshot epoch used to resolve votes and stake
     * @return map from governance action id to aggregated voting data
     */
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
        
        var dRepVotesByProposal = isInConwayBootstrapPhase
            ? Collections.<GovActionId, List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure>>emptyMap()
            : groupVotesByGovAction(dRepVotes);
        var spoVotesByProposal = groupVotesByGovAction(spoVotes);
        var committeeVotesByProposal = groupVotesByGovAction(committeeVotes);

        var dRepEpochAggregates = isInConwayBootstrapPhase ? null : buildDRepEpochAggregates(epoch + 1);
        var spoEpochAggregates = spoVotingDataCollector.buildEpochAggregates(spoVotes, epoch);

        return proposals.stream()
            .collect(Collectors.toMap(
                this::toGovActionId,
                proposal -> createVotingData(
                    proposal,
                    dRepVotesByProposal,
                    spoVotesByProposal,
                    committeeVotesByProposal,
                    isInConwayBootstrapPhase,
                    dRepEpochAggregates,
                    spoEpochAggregates)
            ));
    }

    private AggregatedVotingData createVotingData(GovActionProposal proposal,
                                        Map<GovActionId, List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure>> dRepVotesByProposal,
                                        Map<GovActionId, List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure>> spoVotesByProposal,
                                        Map<GovActionId, List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure>> committeeVotesByProposal,
                                        boolean isInConwayBootstrapPhase,
                                        DRepEpochAggregates dRepEpochAggregates,
                                        SPOVotingDataCollector.SPOEpochAggregates spoEpochAggregates) {
        var govActionId = toGovActionId(proposal);
        var dRepVotesForProposal = dRepVotesByProposal.getOrDefault(govActionId, Collections.emptyList());
        var spoVotesForProposal = spoVotesByProposal.getOrDefault(govActionId, Collections.emptyList());
        var committeeVotesForProposal = committeeVotesByProposal.getOrDefault(govActionId, Collections.emptyList());

        return AggregatedVotingData.builder()
            .drepVotes(isInConwayBootstrapPhase ? createEmptyDRepVotes() : collectDRepVotes(dRepVotesForProposal, dRepEpochAggregates))
            .spoVotes(spoVotingDataCollector.collectSPOVotes(spoVotesForProposal, spoEpochAggregates))
            .committeeVotes(collectCommitteeVotes(committeeVotesForProposal))
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

    private AggregatedVotingData.DRepVotes collectDRepVotes(List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> dRepVotes,
                                                  DRepEpochAggregates context) {
        var noConfidenceStake = context.noConfidenceStake();
        var yesVoteStake = calculateDRepStakeByVote(dRepVotes, Vote.YES, context.epoch());
        var noVoteStake = calculateDRepStakeByVote(dRepVotes, Vote.NO, context.epoch());
        var abstainVoteStake = calculateDRepStakeByVote(dRepVotes, Vote.ABSTAIN, context.epoch());
        var totalDRepStake = context.totalStake();
        var autoAbstainStake = context.autoAbstainStake();
        var doNotVoteStake = totalDRepStake
                .subtract(yesVoteStake)
                .subtract(noVoteStake)
                .subtract(abstainVoteStake)
                .subtract(autoAbstainStake)
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

    private AggregatedVotingData.CommitteeVotes collectCommitteeVotes(
            List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> committeeVotes) {
        if (committeeVotes.isEmpty()) {
            return AggregatedVotingData.CommitteeVotes.builder()
                    .votes(Collections.emptyMap())
                    .build();
        }

        Map<String, Vote> votes = committeeVotes.stream()
            .collect(Collectors.toMap(
                    VotingProcedure::getVoterHash,
                    VotingProcedure::getVote,
                (existing, replacement) -> existing
            ));
        
        return AggregatedVotingData.CommitteeVotes.builder()
            .votes(votes)
            .build();
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

    private Map<GovActionId, List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure>> groupVotesByGovAction(
            List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> votes) {
        if (votes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<GovActionId, List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure>> groupedVotes = votes.stream()
                .collect(Collectors.groupingBy(
                        this::toGovActionId,
                        Collectors.collectingAndThen(Collectors.toList(), List::copyOf)));

        return Map.copyOf(groupedVotes);
    }

    private GovActionId toGovActionId(GovActionProposal proposal) {
        return GovActionId.builder()
                .transactionId(proposal.getTxHash())
                .gov_action_index(proposal.getIndex())
                .build();
    }

    private GovActionId toGovActionId(com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure vote) {
        return GovActionId.builder()
                .transactionId(vote.getGovActionTxHash())
                .gov_action_index(vote.getGovActionIndex())
                .build();
    }

    private DRepEpochAggregates buildDRepEpochAggregates(int epoch) {
        var totalStake = dRepDistStorage.getTotalStakeExcludeInactiveDRepForEpoch(epoch)
                .orElse(BigInteger.ZERO);
        var autoAbstainStake = dRepDistStorage.getStakeByDRepTypeAndEpoch(DrepType.ABSTAIN, epoch)
                .orElse(BigInteger.ZERO);
        var noConfidenceStake = dRepDistStorage.getStakeByDRepTypeAndEpoch(DrepType.NO_CONFIDENCE, epoch)
                .orElse(BigInteger.ZERO);

        return new DRepEpochAggregates(epoch, totalStake, autoAbstainStake, noConfidenceStake);
    }

    private record DRepEpochAggregates(int epoch,
                                       BigInteger totalStake,
                                       BigInteger autoAbstainStake,
                                       BigInteger noConfidenceStake) {
    }
}
