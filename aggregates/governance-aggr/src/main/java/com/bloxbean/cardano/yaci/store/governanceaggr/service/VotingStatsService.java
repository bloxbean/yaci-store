package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMemberDetails;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.AggregatedGovernanceData;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.AggregatedVotingData;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.ProposalVotingStats;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VoteTallyCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class VotingStatsService {

    public Map<GovActionId, ProposalVotingStats> computeVotingStats(AggregatedGovernanceData data) {
        Map<GovActionId, ProposalVotingStats> out = new HashMap<>();
        if (data == null || data.proposalsForEvaluation() == null || data.proposalsForEvaluation().isEmpty()) return out;

        boolean isInBootstrapPhase = data.bootstrapPhase();
        List<CommitteeMemberDetails> committeeMembers = data.committeeMembers();

        for (GovActionProposal proposal : data.proposalsForEvaluation()) {
            GovActionId govActionId = GovActionId.builder()
                    .transactionId(proposal.getTxHash())
                    .gov_action_index(proposal.getIndex())
                    .build();

            AggregatedVotingData aggregatedVotingData = data.aggregatedVotingDataByProposal().get(govActionId);
            GovActionType type = proposal.getGovAction().getType();

            try {
                out.put(govActionId, computeForSingleProposal(aggregatedVotingData, type, committeeMembers, isInBootstrapPhase));
            } catch (Exception e) {
                log.warn("Failed to compute voting stats for {}: {}", govActionId, e.getMessage());
                out.put(govActionId, zeroStats());
            }
        }
        return out;
    }

    private ProposalVotingStats computeForSingleProposal(AggregatedVotingData aggregatedVotingData,
                                                         GovActionType proposalType,
                                                         List<CommitteeMemberDetails> activeCommitteeMembers,
                                                         boolean isBootstrapPhase) {
        if (aggregatedVotingData == null) return zeroStats();

        AggregatedVotingData.DRepVotes dRepVotes = aggregatedVotingData.drepVotes();

        var drepTallies = VoteTallyCalculator.computeDRepTallies(
                VotingData.DRepVotes.builder()
                        .yesVoteStake(dRepVotes.getYesVoteStake())
                        .noVoteStake(dRepVotes.getYesVoteStake())
                        .doNotVoteStake(dRepVotes.getDoNotVoteStake())
                        .noConfidenceStake(dRepVotes.getNoConfidenceStake())
                        .build()
                , proposalType);

        // DRep stats
        BigInteger drepTotalYesStake = drepTallies.getTotalYesStake();
        BigInteger drepTotalNoStake = drepTallies.getTotalNoStake();

        BigInteger drepYesVoteStake = nz(dRepVotes.getYesVoteStake());
        BigInteger drepNoVoteStake = nz(dRepVotes.getNoVoteStake());
        BigInteger drepAbstainVoteStake = nz(dRepVotes.getAbstainVoteStake());

        BigInteger drepAutoAbstainStake = nz(dRepVotes.getAutoAbstainStake());
        BigInteger drepNoConfidenceStake = nz(dRepVotes.getNoConfidenceStake());

        BigInteger drepNotVoteStake = nz(dRepVotes.getDoNotVoteStake());

        BigInteger drepTotalAbstainStake = drepAbstainVoteStake.add(drepAutoAbstainStake);

        // SPO stats
        BigInteger spoTotalYesStake = BigInteger.ZERO;
        BigInteger spoTotalAbstainStake = BigInteger.ZERO;
        BigInteger spoTotalNoStake = BigInteger.ZERO;

        if (aggregatedVotingData.spoVotes() != null) {
            AggregatedVotingData.SPOVotes spoVotes = aggregatedVotingData.spoVotes();

            var spoTallies = VoteTallyCalculator.computeSPOTallies(
                    VotingData.SPOVotes.builder()
                            .yesVoteStake(spoVotes.getYesVoteStake())
                            .abstainVoteStake(spoVotes.getAbstainVoteStake())
                            .doNotVoteStake(spoVotes.getDoNotVoteStake())
                            .delegateToAutoAbstainDRepStake(spoVotes.getDelegateToAutoAbstainDRepStake())
                            .delegateToNoConfidenceDRepStake(spoVotes.getDelegateToNoConfidenceDRepStake())
                            .totalStake(spoVotes.getTotalStake())
                            .build()
                    , proposalType, isBootstrapPhase);

            spoTotalYesStake = spoTallies.getTotalYesStake();
            spoTotalAbstainStake = spoTallies.getTotalAbstainStake();
            spoTotalNoStake = spoTallies.getTotalNoStake();
        }

        BigInteger spoYesVoteStake = aggregatedVotingData.spoVotes() != null ? nz(aggregatedVotingData.spoVotes().getYesVoteStake()) : BigInteger.ZERO;
        BigInteger spoAbstainVoteStake = aggregatedVotingData.spoVotes() != null ? nz(aggregatedVotingData.spoVotes().getAbstainVoteStake()) : BigInteger.ZERO;
        BigInteger spoDoNotVoteStake = aggregatedVotingData.spoVotes() != null ? nz(aggregatedVotingData.spoVotes().getDoNotVoteStake()) : BigInteger.ZERO;

        // Committee stats
        int ccYes = 0, ccNo = 0, ccAbstain = 0, ccDoNotVote = 0;

        if (aggregatedVotingData.committeeVotes() != null && aggregatedVotingData.committeeVotes().getVotes() != null) {

            List<CommitteeMember> members = mapToCommitteeMembers(activeCommitteeMembers);

            var ccTallies = VoteTallyCalculator.computeCommitteeTallies(
                    aggregatedVotingData.committeeVotes().getVotes(),
                    members);

            ccYes = ccTallies.getYesCount();
            ccNo = ccTallies.getNoCount();
            ccAbstain = ccTallies.getAbstainCount();
            ccDoNotVote = ccTallies.getDoNotVoteCount();
        }

        return ProposalVotingStats.builder()
                .spoTotalYesStake(spoTotalYesStake)
                .spoTotalNoStake(spoTotalNoStake)
                .spoTotalAbstainStake(spoTotalAbstainStake)
                .spoYesVoteStake(spoYesVoteStake)
                .spoNoVoteStake(spoTotalNoStake)
                .spoAbstainVoteStake(spoAbstainVoteStake)
                .spoDoNotVoteStake(spoDoNotVoteStake)
                .drepTotalYesStake(drepTotalYesStake)
                .drepTotalNoStake(drepTotalNoStake)
                .drepTotalAbstainStake(drepTotalAbstainStake)
                .drepYesVoteStake(drepYesVoteStake)
                .drepNoVoteStake(drepNoVoteStake)
                .drepAbstainVoteStake(drepAbstainVoteStake)
                .drepNoConfidenceStake(drepNoConfidenceStake)
                .drepAutoAbstainStake(drepAutoAbstainStake)
                .drepDoNotVoteStake(drepNotVoteStake)
                .drepNoConfidenceStake(drepNoConfidenceStake)
                .ccYes(ccYes)
                .ccNo(ccNo)
                .ccAbstain(ccAbstain)
                .ccDoNotVote(ccDoNotVote)
                .build();
    }

    private static List<CommitteeMember> mapToCommitteeMembers(List<CommitteeMemberDetails> details) {
        if (details == null) return List.of();
        return details.stream()
                .filter(Objects::nonNull)
                .map(d -> CommitteeMember.builder()
                        .coldKey(d.getColdKey())
                        .hotKey(d.getHotKey())
                        .startEpoch(d.getStartEpoch())
                        .expiredEpoch(d.getExpiredEpoch())
                        .credType(d.getCredType())
                        .build())
                .toList();
    }

    private static ProposalVotingStats zeroStats() {
        return ProposalVotingStats.builder()
                .spoTotalYesStake(BigInteger.ZERO)
                .spoTotalNoStake(BigInteger.ZERO)
                .spoTotalAbstainStake(BigInteger.ZERO)
                .drepTotalYesStake(BigInteger.ZERO)
                .drepTotalNoStake(BigInteger.ZERO)
                .drepNoVoteStake(BigInteger.ZERO)
                .drepDoNotVoteStake(BigInteger.ZERO)
                .drepNoConfidenceStake(BigInteger.ZERO)
                .drepTotalAbstainStake(BigInteger.ZERO)
                .ccYes(0)
                .ccNo(0)
                .ccAbstain(0)
                .ccDoNotVote(0)
                .build();
    }

    private static BigInteger nz(BigInteger v) {
        return Objects.requireNonNullElse(v, BigInteger.ZERO);
    }
}
