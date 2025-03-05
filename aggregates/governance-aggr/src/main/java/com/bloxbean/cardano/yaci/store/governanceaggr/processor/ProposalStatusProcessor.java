package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.events.domain.ProposalStatusCapturedEvent;
import com.bloxbean.cardano.yaci.store.events.domain.StakeSnapshotTakenEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMemberDetails;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.jackson.CredentialDeserializer;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.GovernanceAggrProperties;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepDist;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.Proposal;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.DRepDistService;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.DelegationVoteDataService;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.VotingAggrService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepDistStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.ProposalMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.util.ProposalUtils;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import com.bloxbean.cardano.yaci.store.governancerules.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.governancerules.domain.RatificationResult;
import com.bloxbean.cardano.yaci.store.governancerules.rule.GovActionRatifier;
import com.bloxbean.cardano.yaci.store.governancerules.util.GovernanceActionUtil;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolDetails;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorageReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
// TODO: write tests
public class ProposalStatusProcessor {
    private final GovActionProposalStatusStorage govActionProposalStatusStorage;
    private final GovActionProposalStorage govActionProposalStorage;
    private final DRepDistService dRepDistService;
    private final ProposalStateClient proposalStateClient;
    private final EpochParamStorage epochParamStorage;
    private final CommitteeStorage committeeStorage;
    private final VotingAggrService votingAggrService;
    private final EpochStakeStorageReader epochStakeStorage;
    private final DRepDistStorageReader dRepDistStorage;
    private final AdaPotStorage adaPotStorage;
    private final CommitteeMemberStorage committeeMemberStorage;
    private final PoolStorage poolStorage;
    private final PoolStorageReader poolStorageReader;
    private final DelegationVoteDataService delegationVoteDataService;
    private final ProposalMapper proposalMapper;
    private final ApplicationEventPublisher publisher;
    private final GovernanceAggrProperties governanceAggrProperties;
    private final StoreProperties storeProperties;
    private boolean isInConwayBootstrapPhase;
    private final ObjectMapper objectMapper;
    private final int QUERY_BATCH_SIZE = 500;

    public ProposalStatusProcessor(GovActionProposalStatusStorage govActionProposalStatusStorage, GovActionProposalStorage govActionProposalStorage,
                                   DRepDistService dRepDistService, ProposalStateClient proposalStateClient,
                                   EpochParamStorage epochParamStorage, CommitteeStorage committeeStorage,
                                   VotingAggrService votingAggrService, EpochStakeStorageReader epochStakeStorage,
                                   DRepDistStorageReader dRepDistStorage, AdaPotStorage adaPotStorage,
                                   CommitteeMemberStorage committeeMemberStorage, PoolStorage poolStorage, PoolStorageReader poolStorageReader, DelegationVoteDataService delegationVoteDataService, ProposalMapper proposalMapper,
                                   ApplicationEventPublisher publisher, GovernanceAggrProperties governanceAggrProperties, StoreProperties storeProperties) {
        this.govActionProposalStatusStorage = govActionProposalStatusStorage;
        this.govActionProposalStorage = govActionProposalStorage;
        this.dRepDistService = dRepDistService;
        this.proposalStateClient = proposalStateClient;
        this.epochParamStorage = epochParamStorage;
        this.committeeStorage = committeeStorage;
        this.votingAggrService = votingAggrService;
        this.epochStakeStorage = epochStakeStorage;
        this.dRepDistStorage = dRepDistStorage;
        this.adaPotStorage = adaPotStorage;
        this.committeeMemberStorage = committeeMemberStorage;
        this.poolStorage = poolStorage;
        this.poolStorageReader = poolStorageReader;
        this.delegationVoteDataService = delegationVoteDataService;
        this.proposalMapper = proposalMapper;
        this.publisher = publisher;
        this.governanceAggrProperties = governanceAggrProperties;
        this.storeProperties = storeProperties;

        this.objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(Credential.class, new CredentialDeserializer());
        this.objectMapper.registerModule(module);

        if (isPublicNetwork()) {
            this.isInConwayBootstrapPhase = true;
        } else {
            this.isInConwayBootstrapPhase = governanceAggrProperties.isDevnetConwayBootstrapAvailable();
        }
    }

    @EventListener
    @Transactional
    // TODO: enactment order
    public void handleProposalStatus(StakeSnapshotTakenEvent stakeSnapshotTakenEvent) {
        int epoch = stakeSnapshotTakenEvent.getEpoch();
        int currentEpoch = epoch + 1;

        // delete records if exists for the epoch
        govActionProposalStatusStorage.deleteByEpoch(currentEpoch);

        var newProposals = govActionProposalStorage.findByEpoch(epoch);

        // get new active proposals in the recent epoch and save them into governance_action_proposal_status table
        govActionProposalStatusStorage.saveAll(newProposals.stream().map(govActionProposal ->
                GovActionProposalStatus.builder()
                        .govActionTxHash(govActionProposal.getTxHash())
                        .govActionIndex((int) govActionProposal.getIndex())
                        .type(govActionProposal.getType())
                        .epoch(epoch)
                        .status(GovActionStatus.ACTIVE)
                        .build()
        ).toList());

        try {
            if (isInConwayBootstrapPhase) {
                // check if there is any enacted hard fork initiation action in the past, if so, the bootstrap phase is over
                var enactedProposal = proposalStateClient.getLastEnactedProposal(GovActionType.HARD_FORK_INITIATION_ACTION, currentEpoch);
                if (enactedProposal.stream().anyMatch(govActionProposalStatus
                        -> govActionProposalStatus.getGovAction().getType().equals(GovActionType.HARD_FORK_INITIATION_ACTION))) {
                    isInConwayBootstrapPhase = false;
                }
            }
            // take dRep stake distribution snapshot
            dRepDistService.takeStakeSnapshot(epoch);

            List<GovActionProposalStatus> govActionProposalStatusListNeedToSave = new ArrayList<>();

            List<GovActionProposal> proposalsForStatusCalculation = getProposalsForStatusCalculation(currentEpoch);

            if (proposalsForStatusCalculation.isEmpty()) {
                return;
            }

            // current epoch param
            var epochParamOpt = epochParamStorage.getProtocolParams(currentEpoch);
            if (epochParamOpt.isEmpty()) {
                log.error("Epoch param not found for epoch: {}", currentEpoch);
                return;
            }
            var currentEpochParam = epochParamOpt.get();

            // current committee
            var committee = committeeStorage.getCommitteeByEpoch(epoch);

            if (committee.isEmpty()) {
                log.error("Committee not found for epoch: {}", epoch);
            }

            // cc threshold
            var ccThreshold = committee.get().getThreshold() == null ? BigDecimal.ZERO : committee.get().getThreshold();

            List<CommitteeMemberDetails> membersCanVote = committeeMemberStorage.getActiveCommitteeMembersDetailsByEpoch(epoch);

            // map (cold key, hot key)
            Map<String, String> coldKeyHotKeyMap = membersCanVote.stream()
                    .collect(Collectors.toMap(CommitteeMemberDetails::getColdKey, CommitteeMemberDetails::getHotKey));

            // map (hot key, cold key list)
            Map<String, List<String>> hotKeyColdKeysMap = membersCanVote.stream()
                    .collect(Collectors.groupingBy(CommitteeMemberDetails::getHotKey,
                            Collectors.mapping(CommitteeMemberDetails::getColdKey, Collectors.toList())));

            // get votes by committee member
            List<VotingProcedure> votesByCommittee = votingAggrService.getVotesByCommittee(epoch, proposalsForStatusCalculation.stream().map(
                    govActionProposal -> GovActionId.builder()
                            .transactionId(govActionProposal.getTxHash())
                            .gov_action_index(govActionProposal.getIndex())
                            .build()).toList(),
                    coldKeyHotKeyMap.values().stream().toList()
                    );

            // get votes by SPO
            List<VotingProcedure> votesBySPO = votingAggrService.getVotesBySPO(epoch, proposalsForStatusCalculation.stream().map(
                    govActionProposal -> GovActionId.builder()
                            .transactionId(govActionProposal.getTxHash())
                            .gov_action_index(govActionProposal.getIndex())
                            .build()).toList());

            // get votes by DRep
            List<VotingProcedure> votesByDRep = new ArrayList<>();

            if (!isInConwayBootstrapPhase) {
                votesByDRep = votingAggrService.getVotesByDRep(epoch, proposalsForStatusCalculation.stream().map(
                        govActionProposal -> GovActionId.builder()
                                .transactionId(govActionProposal.getTxHash())
                                .gov_action_index(govActionProposal.getIndex())
                                .build()).toList());
            }

            // spo total stake
            BigInteger totalPoolStake = epochStakeStorage.getTotalActiveStakeByEpoch(epoch + 2)
                    .orElse(BigInteger.ZERO);

            var enactedProposalsInPrevEpoch = proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, epoch - 2);

            boolean isActionRatificationDelayed = enactedProposalsInPrevEpoch == null || enactedProposalsInPrevEpoch.stream()
                    .anyMatch(govActionProposal-> GovernanceActionUtil.isDelayingAction(govActionProposal.getGovAction().getType()));

            ConstitutionCommitteeState ccState = ConstitutionCommitteeState.NORMAL; //TODO: handle later

            List<String> activePools = poolStorage.findActivePools(epoch).stream()
                    .map(com.bloxbean.cardano.yaci.store.staking.domain.Pool::getPoolId)
                    .toList();

            // map (reward account, List of pools)
            var activePoolsBatches = ListUtil.partition(activePools, QUERY_BATCH_SIZE);
            Map<String, List<String>> rewardAccountPoolMap = activePoolsBatches.parallelStream()
                    .flatMap(batch -> poolStorageReader.getPoolDetails(batch, epoch).stream())
                    .collect(Collectors.groupingBy(PoolDetails::getRewardAccount, Collectors.mapping(PoolDetails::getPoolId, Collectors.toList())));

            // Calculate the total stake of SPOs that delegated to AlwaysAbstain DRep
            List<String> poolsDelegatedToAlwaysAbstainDRep = new ArrayList<>();
            var poolBatches = ListUtil.partition(new ArrayList<>(rewardAccountPoolMap.values()), QUERY_BATCH_SIZE);
            poolBatches.parallelStream().forEach(batch -> delegationVoteDataService
                    .getDelegationVotesByDRepTypeAndAddressList(batch.stream().flatMap(List::stream).toList(), DrepType.ABSTAIN, epoch)
                    .parallelStream()
                    .forEach(delegationVote ->
                            poolsDelegatedToAlwaysAbstainDRep.addAll(rewardAccountPoolMap.get(delegationVote.getAddress()))));

            BigInteger totalStakeSPODelegatedToAbstainDRep = epochStakeStorage
                    .getAllActiveStakesByEpochAndPools(epoch + 2, poolsDelegatedToAlwaysAbstainDRep)
                    .stream()
                    .map(EpochStake::getAmount)
                    .reduce(BigInteger.ZERO, BigInteger::add);

            // Calculate the total stake of SPOs that delegated to NoConfidence DRep
            List<String> poolsDelegatedToNoConfidenceDRep = new ArrayList<>();
            poolBatches.parallelStream().forEach(batch -> delegationVoteDataService
                    .getDelegationVotesByDRepTypeAndAddressList(batch.stream().flatMap(List::stream).toList(), DrepType.NO_CONFIDENCE, epoch)
                    .parallelStream()
                    .forEach(delegationVote ->
                            poolsDelegatedToNoConfidenceDRep.addAll(rewardAccountPoolMap.get(delegationVote.getAddress()))));

            BigInteger totalStakeSPODelegatedToNoConfidenceDRep = epochStakeStorage
                    .getAllActiveStakesByEpochAndPools(epoch + 2, poolsDelegatedToNoConfidenceDRep)
                    .stream()
                    .map(EpochStake::getAmount)
                    .reduce(BigInteger.ZERO, BigInteger::add);

            BigInteger totalDRepStake = BigInteger.ZERO;
            BigInteger dRepAutoAbstainStake = BigInteger.ZERO;

            if (!isInConwayBootstrapPhase) {
                totalDRepStake = dRepDistStorage.getTotalStakeForEpoch(epoch)
                        .orElse(BigInteger.ZERO);
                dRepAutoAbstainStake = dRepDistStorage.getStakeByDRepAndEpoch(DrepType.ABSTAIN.name(), epoch).orElse(BigInteger.ZERO);
            }

            // use gov rule and update proposal status
            for (var proposal : proposalsForStatusCalculation) {
                var govActionDetail = proposal.getGovAction();

                var govActionLifetime = epochParamStorage.getProtocolParams(proposal.getEpoch()).get().getParams().getGovActionLifetime();

                int maxAllowedVotingEpoch = proposal.getEpoch() + govActionLifetime;

                if (govActionDetail.getType().equals(GovActionType.INFO_ACTION)) {
                    GovActionProposalStatus govActionProposalStatus = GovActionProposalStatus
                            .builder()
                            .type(govActionDetail.getType())
                            .govActionTxHash(proposal.getTxHash())
                            .govActionIndex(proposal.getIndex())
                            .epoch(currentEpoch)
                            .status(maxAllowedVotingEpoch < currentEpoch ? GovActionStatus.EXPIRED : GovActionStatus.ACTIVE)
                            .build();
                    govActionProposalStatusListNeedToSave.add(govActionProposalStatus);
                    continue;
                }

                // calculate cc yes vote

                // Many CC Cold Credentials map to the same Hot Credential act as many votes.
                // If the hot credential is compromised at any point, the committee member must generate a new one and issue a new Authorization Certificate.
                // A new Authorization Certificate registered on-chain overrides the previous one, effectively invalidating any votes signed by the old hot credential.

                var validYesVotesByCommittee = votesByCommittee.stream()
                        .filter(votingProcedure ->
                                (votingProcedure.getGovActionTxHash().equals(proposal.getTxHash())
                                        && votingProcedure.getGovActionIndex() == proposal.getIndex()
                                        && votingProcedure.getVote().equals(Vote.YES)
                                        && hotKeyColdKeysMap.containsKey(votingProcedure.getVoterHash()))
                        )
                        .toList();

                int ccYesVote = validYesVotesByCommittee.stream()
                        .mapToInt(vote -> hotKeyColdKeysMap.get(vote.getVoterHash()).size())
                        .sum();

                // calculate cc no vote
                List<CommitteeMemberDetails> committeeMembersDoNotVote = membersCanVote.stream()
                        .filter(committeeMember ->
                                coldKeyHotKeyMap.get(committeeMember.getColdKey()) == null ||
                                        votesByCommittee.stream().noneMatch(
                                                votingProcedure ->
                                                        coldKeyHotKeyMap.get(committeeMember.getColdKey())
                                                                .equals(votingProcedure.getVoterHash())))
                        .toList();

                /*
                    ccNoVote – The total number of committee members that voted 'No' plus the number of committee members that did not vote.
                 */
                var validNoVotesByCommittee = votesByCommittee.stream()
                        .filter(votingProcedure ->
                                votingProcedure.getGovActionTxHash().equals(proposal.getTxHash())
                                        && votingProcedure.getGovActionIndex() == proposal.getIndex()
                                        && votingProcedure.getVote().equals(Vote.NO)
                                        && hotKeyColdKeysMap.containsKey(votingProcedure.getVoterHash())
                        )
                        .toList();

                int ccNoVote = validNoVotesByCommittee.stream()
                        .mapToInt(vote -> hotKeyColdKeysMap.get(vote.getVoterHash()).size())
                        .sum()
                        + committeeMembersDoNotVote.size();

                // calculate The total delegated stake from SPO that voted 'Yes'
                BigInteger spoYesStake = BigInteger.ZERO;

                List<String> poolsVoteYes = votesBySPO.stream()
                        .filter(votingProcedure -> votingProcedure.getVote().equals(Vote.YES)
                                && votingProcedure.getGovActionTxHash().equals(proposal.getTxHash())
                                && votingProcedure.getGovActionIndex() == proposal.getIndex()
                        )
                        .map(VotingProcedure::getVoterHash)
                        .toList();

                if (!poolsVoteYes.isEmpty()) {
                    spoYesStake = epochStakeStorage.getAllActiveStakesByEpochAndPools(epoch + 2, poolsVoteYes)
                            .stream()
                            .map(EpochStake::getAmount)
                            .reduce(BigInteger.ZERO, BigInteger::add);
                }

                // calculate the total delegated stake from SPO that voted 'Abstain'
                BigInteger spoAbstainStake = BigInteger.ZERO;

                List<String> poolsVoteAbstain = votesBySPO.stream()
                        .filter(votingProcedure -> votingProcedure.getVote().equals(Vote.ABSTAIN)
                                && votingProcedure.getGovActionTxHash().equals(proposal.getTxHash())
                                && votingProcedure.getGovActionIndex() == proposal.getIndex()
                        )
                        .map(VotingProcedure::getVoterHash)
                        .toList();

                if (!poolsVoteYes.isEmpty()) {
                    spoAbstainStake = epochStakeStorage.getAllActiveStakesByEpochAndPools(epoch + 2, poolsVoteAbstain)
                            .stream()
                            .map(EpochStake::getAmount)
                            .reduce(BigInteger.ZERO, BigInteger::add);
                }

                // dRep 'yes' stake
                BigInteger dRepYesStake = null;
                // DRep 'no' stake
                BigInteger dRepNoStake = null;

                if (!isInConwayBootstrapPhase) {
                    /* Calculate dRep 'yes' stake */
                    /*
                        dRepYesStake – The total stake of:
                        1. Registered dReps that voted 'Yes', plus
                        2. The AlwaysNoConfidence dRep, in case the action is NoConfidence.
                     */
                    dRepYesStake = BigInteger.ZERO;
                    List<VotingProcedure> votesForThisProposalByDRep = votesByDRep.stream()
                            .filter(votingProcedure -> votingProcedure.getGovActionTxHash().equals(proposal.getTxHash())
                                    && votingProcedure.getGovActionIndex() == proposal.getIndex())
                            .toList();

                    List<String> dRepsVoteYes = votesForThisProposalByDRep.stream()
                            .filter(votingProcedure -> votingProcedure.getVote().equals(Vote.YES))
                            .map(VotingProcedure::getVoterHash)
                            .toList();
                    if (!dRepsVoteYes.isEmpty()) {
                        dRepYesStake = dRepDistStorage.getAllByEpochAndDReps(epoch, dRepsVoteYes)
                                .stream()
                                .map(DRepDist::getAmount)
                                .reduce(BigInteger.ZERO, BigInteger::add);
                    }
                    var dRepNoConfidenceStake = dRepDistStorage.getStakeByDRepAndEpoch(DrepType.NO_CONFIDENCE.name(), epoch);
                    if (govActionDetail.getType().equals(GovActionType.NO_CONFIDENCE) && dRepNoConfidenceStake.isPresent()) {
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

                    List<String> dRepsVoteNo = votesForThisProposalByDRep.stream()
                            .filter(votingProcedure -> votingProcedure.getVote().equals(Vote.NO))
                            .map(VotingProcedure::getVoterHash)
                            .toList();
                    if (!dRepsVoteNo.isEmpty()) {
                        dRepNoStake = dRepDistStorage.getAllByEpochAndDReps(epoch, dRepsVoteNo)
                                .stream()
                                .map(DRepDist::getAmount)
                                .reduce(BigInteger.ZERO, BigInteger::add);
                    }
                    // The total stake of dReps that voted for this action
                    BigInteger totalStakeDRepDoVote = dRepDistStorage.getAllByEpochAndDReps(epoch, votesForThisProposalByDRep.stream()
                                    .map(VotingProcedure::getVoterHash).toList())
                            .stream()
                            .map(DRepDist::getAmount)
                            .reduce(BigInteger.ZERO, BigInteger::add);

                    // The total stake of active dReps that did not vote for this action = totalDRepStake - totalStakeDRepDoVote - dRepAutoAbstainStake - dRepNoConfidenceStake
                    BigInteger totalStakeDRepDoNotVote = totalDRepStake.subtract(totalStakeDRepDoVote).subtract(dRepAutoAbstainStake)
                            .subtract(dRepNoConfidenceStake.orElse(BigInteger.ZERO));

                    dRepNoStake = dRepNoStake.add(totalStakeDRepDoNotVote);

                    if (dRepNoConfidenceStake.isPresent()) {
                        dRepNoStake = dRepNoStake.add(dRepNoConfidenceStake.get());
                    }
                } else {
                    /*
                        During bootstrap phase, For `HardForkInitiation` all SPOs that didn't vote are considered as `No` votes.
                        Whereas, for all other `GovAction`s, SPOs that didn't vote are considered as `Abstain` votes.
                    */
                    if (!govActionDetail.getType().equals(GovActionType.HARD_FORK_INITIATION_ACTION)) {
                        List<String> poolsDoNotVoteForThisAction = activePools.stream()
                                .filter(poolId -> votesBySPO.stream()
                                        .noneMatch(votingProcedure -> votingProcedure.getVoterHash().equals(poolId)
                                                && votingProcedure.getGovActionTxHash().equals(proposal.getTxHash())
                                                && votingProcedure.getGovActionIndex() == proposal.getIndex()))
                                .toList();
                        BigInteger totalStakeSPODoNotVote = epochStakeStorage.getAllActiveStakesByEpochAndPools(epoch + 2, poolsDoNotVoteForThisAction)
                                .stream()
                                .map(EpochStake::getAmount)
                                .reduce(BigInteger.ZERO, BigInteger::add);
                        spoAbstainStake = spoAbstainStake.add(totalStakeSPODoNotVote);
                    }
                }

                /*
                    Cases: a pool delegated to an `AlwaysNoConfidence` or an `AlwaysAbstain` DRep.
                    In those cases, behaviour is as expected: vote `Yes` on `NoConfidence` proposals in case of the former
                    and vote `Abstain` by default in case of the latter
                 */
                if (govActionDetail.getType() == GovActionType.NO_CONFIDENCE) {
                    spoYesStake = spoYesStake.add(totalStakeSPODelegatedToNoConfidenceDRep);
                }
                spoAbstainStake = spoAbstainStake.add(totalStakeSPODelegatedToAbstainDRep);

                // Get the last enacted proposal with the same purpose
                GovActionId lastEnactedGovActionIdWithSamePurpose = proposalStateClient.getLastEnactedProposal(govActionDetail.getType(), currentEpoch)
                        .map(govActionProposal -> GovActionId.builder()
                                .transactionId(govActionProposal.getTxHash())
                                .gov_action_index(govActionProposal.getIndex())
                                .build())
                        .orElse(null);

                // Calculate the treasury
                BigInteger treasury = null;
                if (govActionDetail.getType().equals(GovActionType.TREASURY_WITHDRAWALS_ACTION)) {
                    treasury = adaPotStorage.findByEpoch(currentEpoch)
                            .map(AdaPot::getTreasury).orElse(null);
                }

                try {
                    // get ratification result
                    RatificationResult ratificationResult = GovActionRatifier.getRatificationResult(
                            govActionDetail, isInConwayBootstrapPhase, maxAllowedVotingEpoch, ccYesVote, ccNoVote,
                            ccThreshold, spoYesStake, spoAbstainStake, totalPoolStake,
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
                    } else if (ratificationResult.equals(RatificationResult.REJECT) && maxAllowedVotingEpoch < currentEpoch) {
                        govActionStatus = GovActionStatus.EXPIRED;
                    }

                    GovActionProposalStatus govActionProposalStatus = GovActionProposalStatus
                            .builder()
                            .type(govActionDetail.getType())
                            .govActionTxHash(proposal.getTxHash())
                            .govActionIndex(proposal.getIndex())
                            .epoch(currentEpoch)
                            .status(govActionStatus)
                            .build();
                    govActionProposalStatusListNeedToSave.add(govActionProposalStatus);

                } catch (Exception e) {
                    log.error("Error getting ratification result, epoch: {}", currentEpoch, e);
                }
            }

            if (!govActionProposalStatusListNeedToSave.isEmpty()) {
                govActionProposalStatusStorage.saveAll(govActionProposalStatusListNeedToSave);
            }

        } catch (Exception e) {
            log.info("Error processing proposal status: {}", currentEpoch, e);
        }

        log.info("Finish handling proposal status, current epoch :{}", currentEpoch);

        publisher.publishEvent(new ProposalStatusCapturedEvent(currentEpoch, stakeSnapshotTakenEvent.getSlot()));
    }

    /**
     * Get proposals for status calculation
     * @param epoch current epoch
     * @return
     */
    private List<GovActionProposal> getProposalsForStatusCalculation(int epoch) {
        List<GovActionProposal> activeProposalsInPrevEpoch = proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.ACTIVE, epoch - 1);
        List<GovActionProposal> expiredProposalsInPrevEpoch = proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.EXPIRED,  epoch - 1);
        List<GovActionProposal> ratifiedProposalsInPrevEpoch = proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED,  epoch - 1);

        List<Proposal> expiredProposals = expiredProposalsInPrevEpoch.stream()
                .map(proposalMapper::toProposal)
                .toList();

        List<Proposal> ratifiedProposals = ratifiedProposalsInPrevEpoch.stream()
                .map(proposalMapper::toProposal)
                .toList();

        List<Proposal> activeProposals = activeProposalsInPrevEpoch.stream()
                .map(proposalMapper::toProposal)
                .toList();

        List<Proposal> allProposals = Stream.concat(expiredProposals.stream(), Stream.concat(ratifiedProposals.stream(), activeProposals.stream())).toList();
        Map<GovActionId, Proposal> siblingsOrDescendantsBeDroppedInCurrentEpoch = new HashMap<>();

        for (Proposal proposal : expiredProposals) {
            List<Proposal> proposalsToPrune = ProposalUtils.findDescendants(proposal, allProposals);
            proposalsToPrune.forEach(p -> siblingsOrDescendantsBeDroppedInCurrentEpoch.put(p.getGovActionId(), p));
        }

        for (Proposal proposal : ratifiedProposals) {
            List<Proposal> proposalsToPrune = ProposalUtils.findDescendantsAndSiblings(proposal, allProposals);
            proposalsToPrune.forEach(p -> siblingsOrDescendantsBeDroppedInCurrentEpoch.put(p.getGovActionId(), p));
        }

        return activeProposalsInPrevEpoch.stream()
                .filter(govActionProposal -> !siblingsOrDescendantsBeDroppedInCurrentEpoch.containsKey(
                        GovActionId.builder()
                                .gov_action_index(govActionProposal.getIndex())
                                .transactionId(govActionProposal.getTxHash())
                                .build()))
                .toList();
    }

    private boolean isPublicNetwork() {
        return storeProperties.getProtocolMagic() == Networks.mainnet().getProtocolMagic()
                || storeProperties.getProtocolMagic() == Networks.preprod().getProtocolMagic()
                || storeProperties.getProtocolMagic() == Networks.preview().getProtocolMagic();
    }
}
