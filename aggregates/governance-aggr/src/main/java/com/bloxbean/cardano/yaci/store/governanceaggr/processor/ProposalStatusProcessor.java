package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.actions.*;
import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.events.domain.StakeSnapshotTakenEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.client.ProposalStateClientImpl;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepDist;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.DRepDistService;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.VotingAggrService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepDistStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.rule.GovActionRatifier;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProposalStatusProcessor {
    private final GovActionProposalStatusStorage govActionProposalStatusStorage;
    private final GovActionProposalStorage govActionProposalStorage;
    private final DRepDistService dRepDistService;
    private final ProposalStateClientImpl proposalStateClient;
    private final EpochParamStorage epochParamStorage;
    private final CommitteeStorage committeeStorage;
    private final VotingAggrService votingAggrService;
    private final EpochStakeStorageReader epochStakeStorage;
    private final DRepDistStorageReader dRepDistStorage;
    private final AdaPotStorage adaPotStorage;
    private final CommitteeMemberStorage committeeMemberStorage;
    private boolean isBootstrapPhase = true;

    @EventListener
    @Transactional
    public void handleDuringEpochTransition(PreEpochTransitionEvent event) {
        int prevEpoch = event.getPreviousEpoch() != null ? event.getPreviousEpoch() : event.getEpoch() - 1;
        // get new active proposals in the recent epoch and save them into governance_action_proposal_status table
        var newProposals = govActionProposalStorage.findByEpoch(prevEpoch);

        govActionProposalStatusStorage.saveAll(newProposals.stream().map(govActionProposal ->
                GovActionProposalStatus.builder()
                        .govActionTxHash(govActionProposal.getTxHash())
                        .govActionIndex((int) govActionProposal.getIndex())
                        .type(govActionProposal.getType())
                        .epoch(prevEpoch)
//                        .slot(govActionProposal.getSlot())
                        .status(GovActionStatus.ACTIVE)
                        .build()
        ).toList());
    }

    @EventListener
    @Transactional
    public void handleProposalStatus(StakeSnapshotTakenEvent stakeSnapshotTakenEvent) {
        int prevEpoch = stakeSnapshotTakenEvent.getEpoch();

        try {
            if (isBootstrapPhase) {
                // check if there is any hard fork initiation action in the previous epoch, if so, bootstrap phase is over
                var enactedProposalsInPrevEpoch = govActionProposalStatusStorage.findByStatusAndEpoch(GovActionStatus.ENACTED, prevEpoch);
                if (enactedProposalsInPrevEpoch.stream().anyMatch(govActionProposalStatus
                        -> govActionProposalStatus.getType().equals(GovActionType.HARD_FORK_INITIATION_ACTION))) {
                    isBootstrapPhase = false;
                }
            }

            List<GovActionProposalStatus> govActionProposalStatusListNeedToSave = new ArrayList<>();

            var ratifiedProposals = proposalStateClient.getRatifiedProposals(prevEpoch);
            if (!ratifiedProposals.isEmpty()) {
                ratifiedProposals.forEach(govActionProposal -> {
                    GovActionProposalStatus govActionProposalStatus = GovActionProposalStatus
                            .builder()
                            .type(govActionProposal.getType())
                            .govActionTxHash(govActionProposal.getTxHash())
                            .govActionIndex(govActionProposal.getIndex())
                            .epoch(prevEpoch)
                            .status(GovActionStatus.ENACTED)
                            .build();
                    govActionProposalStatusListNeedToSave.add(govActionProposalStatus);
                });
            }

            // get new active proposals in the recent epoch and save them into governance_action_proposal_status table
            var activeProposals = proposalStateClient.getActiveProposals(prevEpoch);

            if (activeProposals.isEmpty()) {
                return;
            }

            // current epoch param
            var epochParamOpt = epochParamStorage.getProtocolParams(prevEpoch);
            if (epochParamOpt.isEmpty()) {
                //TODO
            }
            var currentEpochParam = epochParamOpt.get();

            // current committee
            var committee = committeeStorage.getCommitteeByEpoch(prevEpoch);

            if (committee.isEmpty()) {
                //TODO
            }

            // cc threshold
            var ccThreshold = committee.get().getThreshold();
            // take dRep stake distribution snapshot
            dRepDistService.takeStakeSnapshot(stakeSnapshotTakenEvent.getEpoch());

            // get votes by committee member
            List<VotingProcedure> votesByCommittee = votingAggrService.getVotesByCommittee(prevEpoch, activeProposals.stream().map(
                    govActionProposal -> GovActionId.builder()
                            .transactionId(govActionProposal.getTxHash())
                            .gov_action_index(govActionProposal.getIndex())
                            .build()).toList());

            // get votes by SPO
            List<VotingProcedure> votesBySPO = votingAggrService.getVotesBySPO(prevEpoch, activeProposals.stream().map(
                    govActionProposal -> GovActionId.builder()
                            .transactionId(govActionProposal.getTxHash())
                            .gov_action_index(govActionProposal.getIndex())
                            .build()).toList());

            // get votes by DRep
            List<VotingProcedure> votesByDRep = new ArrayList<>();

            if (!isBootstrapPhase) {
                votesByDRep = votingAggrService.getVotesByDRep(prevEpoch, activeProposals.stream().map(
                        govActionProposal -> GovActionId.builder()
                                .transactionId(govActionProposal.getTxHash())
                                .gov_action_index(govActionProposal.getIndex())
                                .build()).toList());
            }

            // spo total stake
            BigInteger totalPoolStake = epochStakeStorage.getTotalActiveStakeByEpoch(prevEpoch)
                    .orElse(BigInteger.ZERO);

            var enactedProposalsInPrevEpoch = govActionProposalStatusStorage.findByStatusAndEpoch(GovActionStatus.ENACTED, prevEpoch);

            boolean isActionRatificationDelayed = enactedProposalsInPrevEpoch == null || enactedProposalsInPrevEpoch.stream()
                    .anyMatch(govActionProposalStatus -> GovernanceActionUtil.isDelayingAction(govActionProposalStatus.getType()));
            ConstitutionCommitteeState ccState = ConstitutionCommitteeState.NORMAL; //TODO: handle later

            List<CommitteeMember> committeeMembers = committeeMemberStorage.getCommitteeMembersByEpoch(prevEpoch);

            // use gov rule and update proposal status
            for (var proposal : activeProposals) {
                var govActionLifetime = epochParamStorage.getProtocolParams(proposal.getEpoch()).get().getParams().getGovActionLifetime();

                int expiredEpoch = proposal.getEpoch() + govActionLifetime;

                if (proposal.getType().equals(GovActionType.INFO_ACTION)) {
                    GovActionProposalStatus govActionProposalStatus = GovActionProposalStatus
                            .builder()
                            .type(proposal.getType())
                            .govActionTxHash(proposal.getTxHash())
                            .govActionIndex(proposal.getIndex())
                            .epoch(stakeSnapshotTakenEvent.getEpoch() + 1)
                            .status(expiredEpoch < (prevEpoch + 1) ? GovActionStatus.EXPIRED : GovActionStatus.ACTIVE)
                            .build();
                    govActionProposalStatusListNeedToSave.add(govActionProposalStatus);
                    continue;
                }

                // calculate cc yes vote
                int ccYesVote = votesByCommittee.stream()
                        .filter(votingProcedure -> votingProcedure.getGovActionTxHash().equals(proposal.getTxHash())
                                && votingProcedure.getGovActionIndex() == proposal.getIndex()
                                && votingProcedure.getVote().equals(Vote.YES)).toList().size();
                // calculate cc no vote
                List<CommitteeMember> committeeMembersDoNotVote = committeeMembers.stream()
                        .filter(committeeMember -> votesByCommittee.stream()
                                .noneMatch(votingProcedure -> votingProcedure.getVoterHash().equals(committeeMember.getHash())))
                        .toList();
                /*
                    ccNoVote – The total number of committee members that voted 'No' plus the number of committee members that did not vote.
                 */
                int ccNoVote = votesByCommittee.stream()
                        .filter(votingProcedure -> votingProcedure.getGovActionTxHash().equals(proposal.getTxHash())
                                && votingProcedure.getGovActionIndex() == proposal.getIndex()
                                && votingProcedure.getVote().equals(Vote.NO)).toList().size()
                        + committeeMembersDoNotVote.size();

                // calculate The total delegated stake from SPO that voted 'Yes'
                BigInteger spoYesStake = BigInteger.ZERO;

                List<String> poolsVoteYes = votesBySPO.stream()
                        .filter(votingProcedure -> votingProcedure.getVote().equals(Vote.YES))
                        .map(VotingProcedure::getVoterHash)
                        .toList();

                if (!poolsVoteYes.isEmpty()) {
                    spoYesStake = epochStakeStorage.getAllActiveStakesByEpochAndPools(prevEpoch, poolsVoteYes)
                            .stream()
                            .map(EpochStake::getAmount)
                            .reduce(BigInteger.ZERO, BigInteger::add);
                }

                // calculate The total delegated stake from SPO that voted 'Abstain'
                BigInteger spoAbstainStake = BigInteger.ZERO;

                List<String> poolsVoteAbstain = votesBySPO.stream()
                        .map(VotingProcedure::getVoterHash)
                        .toList();

                if (!poolsVoteYes.isEmpty()) {
                    spoAbstainStake = epochStakeStorage.getAllActiveStakesByEpochAndPools(prevEpoch, poolsVoteAbstain)
                            .stream()
                            .map(EpochStake::getAmount)
                            .reduce(BigInteger.ZERO, BigInteger::add);
                }

                // dRep 'yes' stake
                BigInteger dRepYesStake = null;
                // DRep 'no' stake
                BigInteger dRepNoStake = null;

                if (!isBootstrapPhase) {

                    /* Calculate dRep 'yes' stake */
                    /*
                        dRepYesStake – The total stake of:
                        1. Registered dReps that voted 'Yes', plus
                        2. The AlwaysNoConfidence dRep, in case the action is NoConfidence.
                     */
                    dRepYesStake = BigInteger.ZERO;

                    List<String> dRepsVoteYes = votesByDRep.stream()
                            .filter(votingProcedure -> votingProcedure.getVote().equals(Vote.YES))
                            .map(VotingProcedure::getVoterHash)
                            .toList();
                    if (!dRepsVoteYes.isEmpty()) {
                        dRepYesStake = dRepDistStorage.getAllByEpochAndDReps(prevEpoch, dRepsVoteYes)
                                .stream()
                                .map(DRepDist::getAmount)
                                .reduce(BigInteger.ZERO, BigInteger::add);
                    }
                    var dRepNoConfidenceStake = dRepDistStorage.getStakeByDRepAndEpoch(DrepType.NO_CONFIDENCE.name(), prevEpoch);
                    if (proposal.getType().equals(GovActionType.NO_CONFIDENCE) && dRepNoConfidenceStake.isPresent()) {
                        dRepYesStake = dRepYesStake.add(dRepNoConfidenceStake.get());
                    }

                    /* Calculate dRep 'no' stake */
                    /*
                        dRepNoStake – The total stake of:
                        1. Registered dReps that voted 'No', plus
                        2. Registered dReps that did not vote for this action, plus
                        3. The AlwaysNoConfidence dRep.
                     */
                    dRepNoStake = BigInteger.ZERO;

                    List<String> dRepsVoteNo = votesByDRep.stream()
                            .filter(votingProcedure -> votingProcedure.getVote().equals(Vote.NO))
                            .map(VotingProcedure::getVoterHash)
                            .toList();
                    if (!dRepsVoteNo.isEmpty()) {
                        dRepNoStake = dRepDistStorage.getAllByEpochAndDReps(prevEpoch, dRepsVoteNo)
                                .stream()
                                .map(DRepDist::getAmount)
                                .reduce(BigInteger.ZERO, BigInteger::add);
                    }
                    // The total stake of dReps that voted for this action
                    BigInteger totalStakeDRepDoVote = dRepDistStorage.getAllByEpochAndDReps(prevEpoch, votesByDRep.stream()
                                    .map(VotingProcedure::getVoterHash).toList())
                            .stream()
                            .map(DRepDist::getAmount)
                            .reduce(BigInteger.ZERO, BigInteger::add);
                    BigInteger totalDRepStake = dRepDistStorage.getTotalStakeForEpoch(prevEpoch)
                            .orElse(BigInteger.ZERO);
                    var dRepAutoAbstainStake = dRepDistStorage.getStakeByDRepAndEpoch(DrepType.ABSTAIN.name(), prevEpoch);

                    // The total stake of active dReps that did not vote for this action = totalDRepStake - totalStakeDRepDoVote - dRepAutoAbstainStake - dRepNoConfidenceStake
                    BigInteger totalStakeDRepDoNotVote = totalDRepStake.subtract(totalStakeDRepDoVote).subtract(dRepAutoAbstainStake.orElse(BigInteger.ZERO))
                            .subtract(dRepNoConfidenceStake.orElse(BigInteger.ZERO));

                    dRepNoStake = dRepNoStake.add(totalStakeDRepDoNotVote);

                    if (dRepNoConfidenceStake.isPresent()) {
                        dRepNoStake = dRepNoStake.add(dRepNoConfidenceStake.get());
                    }

                }

                // Get the last enacted proposal with the same purpose
                GovActionId lastEnactedGovActionIdWithSamePurpose = govActionProposalStatusStorage.findLastEnactedProposal(proposal.getType())
                        .map(govActionProposalStatus -> GovActionId.builder()
                                .transactionId(govActionProposalStatus.getGovActionTxHash())
                                .gov_action_index(govActionProposalStatus.getGovActionIndex())
                                .build())
                        .orElse(null);

                // Calculate the treasury
                BigInteger treasury = null;
                if (proposal.getType().equals(GovActionType.TREASURY_WITHDRAWALS_ACTION)) {
                    treasury = adaPotStorage.findByEpoch(prevEpoch)
                            .map(AdaPot::getTreasury).orElse(null);
                }

                try {
                    GovAction govAction = convertToGovAction(proposal);

                    // get ratification result
                    RatificationResult ratificationResult = GovActionRatifier.getRatificationResult(
                            govAction, isBootstrapPhase, expiredEpoch, ccYesVote, ccNoVote,
                            BigDecimal.valueOf(ccThreshold), spoYesStake, spoAbstainStake, totalPoolStake,
                            dRepYesStake, dRepNoStake, ccState, lastEnactedGovActionIdWithSamePurpose, isActionRatificationDelayed,
                            treasury,
                            EpochParam.builder()
                                    .epoch(currentEpochParam.getEpoch())
                                    .params(currentEpochParam.getParams())
                                    .slot(currentEpochParam.getSlot())
                                    .build());

                    GovActionStatus govActionStatus = GovActionStatus.ACTIVE;
                    if (ratificationResult.equals(RatificationResult.ACCEPT)) {
                        govActionStatus = GovActionStatus.RATIFIED;
                    } else if (ratificationResult.equals(RatificationResult.REJECT) && proposal.getEpoch() + govActionLifetime > prevEpoch) {
                        govActionStatus = GovActionStatus.EXPIRED;
                    }

                    GovActionProposalStatus govActionProposalStatus = GovActionProposalStatus
                            .builder()
                            .type(proposal.getType())
                            .govActionTxHash(proposal.getTxHash())
                            .govActionIndex(proposal.getIndex())
                            .epoch(stakeSnapshotTakenEvent.getEpoch() + 1)
                            .status(govActionStatus)
                            .build();
                    govActionProposalStatusListNeedToSave.add(govActionProposalStatus);
                } catch (Exception e) {
                    log.error("Error getting ratification result", e);
                }
            }

            if (!govActionProposalStatusListNeedToSave.isEmpty()) {
                govActionProposalStatusStorage.saveAll(govActionProposalStatusListNeedToSave);
            }
        } catch (Exception e) {
            log.info("Error processing proposal status: {}", prevEpoch + 1, e);
        }
    }

    private GovAction convertToGovAction(GovActionProposal govActionProposal) throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        return switch (govActionProposal.getType()) {
            case INFO_ACTION -> new InfoAction();
            case HARD_FORK_INITIATION_ACTION ->
                    objectMapper.treeToValue(govActionProposal.getDetails(), HardForkInitiationAction.class);
            case TREASURY_WITHDRAWALS_ACTION ->
                    objectMapper.treeToValue(govActionProposal.getDetails(), TreasuryWithdrawalsAction.class);
            case NO_CONFIDENCE -> objectMapper.treeToValue(govActionProposal.getDetails(), NoConfidence.class);
            case UPDATE_COMMITTEE -> objectMapper.treeToValue(govActionProposal.getDetails(), UpdateCommittee.class);
            case NEW_CONSTITUTION -> objectMapper.treeToValue(govActionProposal.getDetails(), NewConstitution.class);
            case PARAMETER_CHANGE_ACTION ->
                    objectMapper.treeToValue(govActionProposal.getDetails(), ParameterChangeAction.class);
        };
    }
}

