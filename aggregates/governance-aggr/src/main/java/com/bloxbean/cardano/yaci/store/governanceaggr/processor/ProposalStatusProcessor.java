package com.bloxbean.cardano.yaci.store.governanceaggr.processor;

import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.governance.*;
import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobExtraInfo;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.processor.EraGenesisProtocolParamsUtil;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.events.domain.ProposalStatusCapturedEvent;
import com.bloxbean.cardano.yaci.store.events.domain.StakeSnapshotTakenEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMemberDetails;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.jackson.CredentialDeserializer;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepDist;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.Proposal;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.ProposalVotingStats;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.CommitteeStateService;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.DRepDistService;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.DelegationVoteDataService;
import com.bloxbean.cardano.yaci.store.governanceaggr.service.VotingAggrService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepDistStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.ProposalMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.util.DRepUtil;
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
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bloxbean.cardano.yaci.store.governanceaggr.GovernanceAggrConfiguration.STORE_GOVERNANCEAGGR_ENABLED;

@Component
@EnableIf(value = STORE_GOVERNANCEAGGR_ENABLED, defaultValue = false)
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
    private final CommitteeStateService committeeStateService;
    private final EpochStakeStorageReader epochStakeStorage;
    private final DRepDistStorageReader dRepDistStorage;
    private final AdaPotStorage adaPotStorage;
    private final CommitteeMemberStorage committeeMemberStorage;
    private final PoolStorage poolStorage;
    private final PoolStorageReader poolStorageReader;
    private final DelegationVoteDataService delegationVoteDataService;
    private final EraService eraService;
    private final AdaPotJobStorage adaPotJobStorage;
    private final ProposalMapper proposalMapper;
    private final ApplicationEventPublisher publisher;
    private final StoreProperties storeProperties;
    private final EraGenesisProtocolParamsUtil eraGenesisProtocolParamsUtil;
    private final ObjectMapper objectMapper;
    private final int QUERY_BATCH_SIZE = 500;

    public ProposalStatusProcessor(GovActionProposalStatusStorage govActionProposalStatusStorage, GovActionProposalStorage govActionProposalStorage,
                                   DRepDistService dRepDistService, ProposalStateClient proposalStateClient,
                                   EpochParamStorage epochParamStorage, CommitteeStorage committeeStorage,
                                   VotingAggrService votingAggrService, CommitteeStateService committeeStateService,
                                   EpochStakeStorageReader epochStakeStorage, DRepDistStorageReader dRepDistStorage,
                                   AdaPotStorage adaPotStorage, CommitteeMemberStorage committeeMemberStorage,
                                   PoolStorage poolStorage, PoolStorageReader poolStorageReader, DelegationVoteDataService delegationVoteDataService, EraService eraService, AdaPotJobStorage adaPotJobStorage, ProposalMapper proposalMapper,
                                   ApplicationEventPublisher publisher, StoreProperties storeProperties, EraGenesisProtocolParamsUtil eraGenesisProtocolParamsUtil) {
        this.govActionProposalStatusStorage = govActionProposalStatusStorage;
        this.govActionProposalStorage = govActionProposalStorage;
        this.dRepDistService = dRepDistService;
        this.proposalStateClient = proposalStateClient;
        this.epochParamStorage = epochParamStorage;
        this.committeeStorage = committeeStorage;
        this.votingAggrService = votingAggrService;
        this.committeeStateService = committeeStateService;
        this.epochStakeStorage = epochStakeStorage;
        this.dRepDistStorage = dRepDistStorage;
        this.adaPotStorage = adaPotStorage;
        this.committeeMemberStorage = committeeMemberStorage;
        this.poolStorage = poolStorage;
        this.poolStorageReader = poolStorageReader;
        this.delegationVoteDataService = delegationVoteDataService;
        this.eraService = eraService;
        this.adaPotJobStorage = adaPotJobStorage;
        this.proposalMapper = proposalMapper;
        this.publisher = publisher;
        this.storeProperties = storeProperties;
        this.eraGenesisProtocolParamsUtil = eraGenesisProtocolParamsUtil;

        this.objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(Credential.class, new CredentialDeserializer());
        this.objectMapper.registerModule(module);
    }

    @EventListener
    @Transactional
    // TODO: enactment order
    public void handleProposalStatus(StakeSnapshotTakenEvent stakeSnapshotTakenEvent) {
        if (eraService.getEraForEpoch(stakeSnapshotTakenEvent.getEpoch()).getValue() < Era.Conway.getValue()) {
            return;
        }
        int epoch = stakeSnapshotTakenEvent.getEpoch();
        int currentEpoch = epoch + 1;

        // delete records if exists for the epoch
        govActionProposalStatusStorage.deleteByEpoch(currentEpoch);

        takeDRepDistrSnapshot(currentEpoch);

        var start = Instant.now();

        var startTime = Instant.now();
        List<GovActionProposalStatus> govActionProposalStatusListNeedToSave = evaluateProposalStatus(currentEpoch);
        var endTime = Instant.now();
        log.info("Evaluate proposal status time: {} ms", endTime.toEpochMilli() - startTime.toEpochMilli());

        if (govActionProposalStatusListNeedToSave == null) return;

        if (!govActionProposalStatusListNeedToSave.isEmpty()) {
            govActionProposalStatusStorage.saveAll(govActionProposalStatusListNeedToSave);
        }
        endTime = Instant.now();
        log.info("Processing and Save proposal status time: {} ms", endTime.toEpochMilli() - startTime.toEpochMilli());

        log.info("Finish handling proposal status, current epoch :{}", currentEpoch);
        publisher.publishEvent(new ProposalStatusCapturedEvent(currentEpoch, stakeSnapshotTakenEvent.getSlot()));

        var end = Instant.now();

        adaPotJobStorage.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, currentEpoch)
                .ifPresent(adaPotJob -> {
                    AdaPotJobExtraInfo extraInfo = adaPotJob.getExtraInfo();

                    if (extraInfo == null) {
                        extraInfo = AdaPotJobExtraInfo.builder()
                                .drepExpiryCalcTime(0L)
                                .govActionStatusCalcTime(0L)
                                .build();
                    }

                    extraInfo.setGovActionStatusCalcTime(end.toEpochMilli() - start.toEpochMilli());
                    adaPotJob.setExtraInfo(extraInfo);

                    adaPotJobStorage.save(adaPotJob);
                });
    }

    public List<GovActionProposalStatus> evaluateProposalStatus(int currentEpoch) {
        int prevEpoch = currentEpoch - 1;

        final boolean isInConwayBootstrapPhase = isEpochInConwayBootstrapPhase(currentEpoch);

        List<GovActionProposalStatus> govActionProposalStatusListNeedToSave = new ArrayList<>();

        List<GovActionProposal> proposalsForStatusCalculation = getProposalsForStatusCalculation(currentEpoch);

        if (proposalsForStatusCalculation.isEmpty()) {
            return Collections.emptyList();
        }

        // TODO: add tx_index in governance_action proposal and sort
        List<GovActionProposal> sortedProposalsForStatusCalculation = proposalsForStatusCalculation.stream()
                .sorted(Comparator.comparingInt((GovActionProposal proposal) -> GovernanceActionUtil.getActionPriority(proposal.getGovAction().getType()))
                        .thenComparingInt(GovActionProposal::getEpoch)
                        .thenComparingLong(GovActionProposal::getSlot))
                .toList();

        // current epoch param
        var epochParamOpt = epochParamStorage.getProtocolParams(currentEpoch);
        if (epochParamOpt.isEmpty()) {
            log.error("Epoch param not found for epoch: {}", currentEpoch);
            return Collections.emptyList();
        }
        var currentEpochParam = epochParamOpt.get();

        // current committee
        var committee = committeeStorage.getCommitteeByEpoch(prevEpoch);

        if (committee.isEmpty()) {
            log.error("Committee not found for epoch: {}", prevEpoch);
        }

        // cc threshold
        var ccThreshold = committee.get().getThreshold() == null ? BigDecimal.ZERO : committee.get().getThreshold();
        long start = System.currentTimeMillis();

        List<CommitteeMemberDetails> membersCanVote = committeeMemberStorage.getActiveCommitteeMembersDetailsByEpoch(prevEpoch);
        long end = System.currentTimeMillis();
        log.debug("GetMembersCanVote time: {} ms", end - start);

        // map (cold key, hot key)
        Map<String, String> coldKeyHotKeyMap = membersCanVote.stream()
                .collect(Collectors.toMap(CommitteeMemberDetails::getColdKey, CommitteeMemberDetails::getHotKey));

        // map (hot key, cold key list)
        Map<String, List<String>> hotKeyColdKeysMap = membersCanVote.stream()
                .collect(Collectors.groupingBy(CommitteeMemberDetails::getHotKey,
                        Collectors.mapping(CommitteeMemberDetails::getColdKey, Collectors.toList())));
        start = System.currentTimeMillis();
        // get votes by committee member
        List<VotingProcedure> votesByCommittee = votingAggrService.getVotesByCommittee(prevEpoch, sortedProposalsForStatusCalculation.stream().map(
                        govActionProposal -> GovActionId.builder()
                                .transactionId(govActionProposal.getTxHash())
                                .gov_action_index(govActionProposal.getIndex())
                                .build()).toList(),
                coldKeyHotKeyMap.values().stream().toList()
        );
        end = System.currentTimeMillis();

        log.debug("GetVotesByCommittee time: {} ms", end - start);
        // get votes by SPO
        start = System.currentTimeMillis();
        List<VotingProcedure> votesBySPO = votingAggrService.getVotesBySPO(prevEpoch, sortedProposalsForStatusCalculation.stream().map(
                govActionProposal -> GovActionId.builder()
                        .transactionId(govActionProposal.getTxHash())
                        .gov_action_index(govActionProposal.getIndex())
                        .build()).toList());
        end = System.currentTimeMillis();
        log.debug("GetVotesBySPO time: {} ms", end - start);

        // get votes by DRep
        start = System.currentTimeMillis();
        List<VotingProcedure> votesByDRep = new ArrayList<>();

        if (!isInConwayBootstrapPhase) {
            votesByDRep = votingAggrService.getVotesByDRep(prevEpoch, sortedProposalsForStatusCalculation.stream().map(
                    govActionProposal -> GovActionId.builder()
                            .transactionId(govActionProposal.getTxHash())
                            .gov_action_index(govActionProposal.getIndex())
                            .build()).toList());
        }
        end = System.currentTimeMillis();
        log.debug("GetVotesByDRep time: {} ms", end - start);

        start = System.currentTimeMillis();
        // spo total stake
        BigInteger totalPoolStake = epochStakeStorage.getTotalActiveStakeByEpoch(prevEpoch + 2)
                .orElse(BigInteger.ZERO);
        end = System.currentTimeMillis();
        log.debug("GetTotalActiveStakeByEpoch time: {} ms", end - start);

        ConstitutionCommitteeState ccState = committeeStateService.getCurrentCommitteeState(); //TODO: handle later

        start = System.currentTimeMillis();
        List<String> activePools = poolStorage.findActivePools(prevEpoch).stream()
                .map(com.bloxbean.cardano.yaci.store.staking.domain.Pool::getPoolId)
                .toList();
        end = System.currentTimeMillis();
        log.debug("GetActivePools time: {} ms", end - start);

        start = System.currentTimeMillis();
        // map (reward account, List of pools)
        var activePoolsBatches = ListUtil.partition(activePools, QUERY_BATCH_SIZE);
        Map<String, List<String>> rewardAccountPoolMap = activePoolsBatches.parallelStream()
                .flatMap(batch -> poolStorageReader.getPoolDetails(batch, prevEpoch).stream())
                .collect(Collectors.groupingBy(PoolDetails::getRewardAccount, Collectors.mapping(PoolDetails::getPoolId, Collectors.toList())));
        end = System.currentTimeMillis();
        log.debug("GetPoolDetails time: {} ms", end - start);

        start = System.currentTimeMillis();
        // Calculate the total stake of SPOs that delegated to AlwaysAbstain DRep
        List<String> poolsDelegatedToAlwaysAbstainDRep = new ArrayList<>();
        var poolBatches = ListUtil.partition(new ArrayList<>(rewardAccountPoolMap.values()), QUERY_BATCH_SIZE);
        poolBatches.parallelStream().forEach(batch -> delegationVoteDataService
                .getDelegationVotesByDRepTypeAndAddressList(batch.stream().flatMap(List::stream).toList(), DrepType.ABSTAIN, prevEpoch)
                .parallelStream()
                .forEach(delegationVote ->
                        poolsDelegatedToAlwaysAbstainDRep.addAll(rewardAccountPoolMap.get(delegationVote.getAddress()))));
        end = System.currentTimeMillis();
        log.debug("GetDelegationVotesByDRepTypeAndAddressList time: {} ms", end - start);

        start = System.currentTimeMillis();
        BigInteger totalStakeSPODelegatedToAbstainDRep = epochStakeStorage
                .getAllActiveStakesByEpochAndPools(prevEpoch + 2, poolsDelegatedToAlwaysAbstainDRep)
                .stream()
                .map(EpochStake::getAmount)
                .reduce(BigInteger.ZERO, BigInteger::add);
        end = System.currentTimeMillis();
        log.debug("GetAllActiveStakesByEpochAndPools time: {} ms", end - start);

        start = System.currentTimeMillis();
        // Calculate the total stake of SPOs that delegated to NoConfidence DRep
        List<String> poolsDelegatedToNoConfidenceDRep = new ArrayList<>();
        poolBatches.parallelStream().forEach(batch -> delegationVoteDataService
                .getDelegationVotesByDRepTypeAndAddressList(batch.stream().flatMap(List::stream).toList(), DrepType.NO_CONFIDENCE, prevEpoch)
                .parallelStream()
                .forEach(delegationVote ->
                        poolsDelegatedToNoConfidenceDRep.addAll(rewardAccountPoolMap.get(delegationVote.getAddress()))));
        end = System.currentTimeMillis();
        log.debug("GetDelegationVotesByDRepTypeAndAddressList time: {} ms", end - start);

        start = System.currentTimeMillis();
        BigInteger totalStakeSPODelegatedToNoConfidenceDRep = epochStakeStorage
                .getAllActiveStakesByEpochAndPools(prevEpoch + 2, poolsDelegatedToNoConfidenceDRep)
                .stream()
                .map(EpochStake::getAmount)
                .reduce(BigInteger.ZERO, BigInteger::add);
        end = System.currentTimeMillis();
        log.debug("GetAllActiveStakesByEpochAndPools time: {} ms", end - start);

        BigInteger totalDRepStake = BigInteger.ZERO;
        BigInteger dRepAutoAbstainStake = BigInteger.ZERO;

        start = System.currentTimeMillis();
        if (!isInConwayBootstrapPhase) {
            totalDRepStake = dRepDistStorage.getTotalStakeExcludeInactiveDRepForEpoch(currentEpoch)
                    .orElse(BigInteger.ZERO);
            dRepAutoAbstainStake = dRepDistStorage.getStakeByDRepTypeAndEpoch(DrepType.ABSTAIN, currentEpoch).orElse(BigInteger.ZERO);
        }

        end = System.currentTimeMillis();
        log.debug("GetTotalStakeForEpoch & getSTakeByDRepTypeAndEpoch time: {} ms", end - start);

        boolean isActionRatificationDelayed = false;

        long loopStart = System.currentTimeMillis();
        // use gov rule and update proposal status
        for (var proposal : sortedProposalsForStatusCalculation) {
            var govActionDetail = proposal.getGovAction();

            ProposalVotingStats votingStats = initProposalVotingStats();

            votingStats.setDrepTotalAbstainStake(dRepAutoAbstainStake);

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
            votingStats.setCcYes(ccYesVote);

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
            votingStats.setCcNo(ccNoVote - committeeMembersDoNotVote.size());
            votingStats.setCcDoNotVote(committeeMembersDoNotVote.size());

                /*
                    ccAbstainVote – The total number of committee members that voted 'Abstain' .
                 */
            var validAbstainVotesByCommittee = votesByCommittee.stream()
                    .filter(votingProcedure ->
                            votingProcedure.getGovActionTxHash().equals(proposal.getTxHash())
                                    && votingProcedure.getGovActionIndex() == proposal.getIndex()
                                    && votingProcedure.getVote().equals(Vote.ABSTAIN)
                                    && hotKeyColdKeysMap.containsKey(votingProcedure.getVoterHash())
                    )
                    .toList();
            int ccAbstainVote = validAbstainVotesByCommittee.stream()
                    .mapToInt(vote -> hotKeyColdKeysMap.get(vote.getVoterHash()).size())
                    .sum();
            votingStats.setCcAbstain(ccAbstainVote);

            // calculate The total delegated stake from SPO that voted 'Yes'
            BigInteger spoYesStake = BigInteger.ZERO;

            List<String> poolsVoteYes = votesBySPO.stream()
                    .filter(votingProcedure -> votingProcedure.getVote().equals(Vote.YES)
                            && votingProcedure.getGovActionTxHash().equals(proposal.getTxHash())
                            && votingProcedure.getGovActionIndex() == proposal.getIndex()
                    )
                    .map(VotingProcedure::getVoterHash)
                    .toList();

            start = System.currentTimeMillis();
            if (!poolsVoteYes.isEmpty()) {
                spoYesStake = epochStakeStorage.getAllActiveStakesByEpochAndPools(prevEpoch + 2, poolsVoteYes)
                        .stream()
                        .map(EpochStake::getAmount)
                        .reduce(BigInteger.ZERO, BigInteger::add);
            }
            end = System.currentTimeMillis();
            log.debug("GetAllActiveStakesByEpochAndPools (in loop) time: {} ms", end - start);

            // calculate the total delegated stake from SPO that voted 'Abstain'
            BigInteger spoAbstainStake = BigInteger.ZERO;

            List<String> poolsVoteAbstain = votesBySPO.stream()
                    .filter(votingProcedure -> votingProcedure.getVote().equals(Vote.ABSTAIN)
                            && votingProcedure.getGovActionTxHash().equals(proposal.getTxHash())
                            && votingProcedure.getGovActionIndex() == proposal.getIndex()
                    )
                    .map(VotingProcedure::getVoterHash)
                    .toList();

            start = System.currentTimeMillis();
            if (!poolsVoteYes.isEmpty()) {
                spoAbstainStake = epochStakeStorage.getAllActiveStakesByEpochAndPools(prevEpoch + 2, poolsVoteAbstain)
                        .stream()
                        .map(EpochStake::getAmount)
                        .reduce(BigInteger.ZERO, BigInteger::add);
            }
            end = System.currentTimeMillis();
            log.debug("GetAllActiveStakesByEpochAndPools (in loop -- poolsVoteAbstain) time: {} ms", end - start);

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
                        .map(DRepUtil::getDRepId)
                        .filter(Objects::nonNull)
                        .toList();

                start = System.currentTimeMillis();
                if (!dRepsVoteYes.isEmpty()) {
                    dRepYesStake = dRepDistStorage.getAllByEpochAndDRepIdsExcludeInactiveDReps(currentEpoch, dRepsVoteYes)
                            .stream()
                            .map(DRepDist::getAmount)
                            .reduce(BigInteger.ZERO, BigInteger::add);
                }
                end = System.currentTimeMillis();
                log.debug("GetAllByEpochAndDRepIds (drep vote yes) time: {} ms", end - start);

                start = System.currentTimeMillis();
                // The total stake of No Confidence DRep
                var dRepNoConfidenceStake = dRepDistStorage.getStakeByDRepTypeAndEpoch(DrepType.NO_CONFIDENCE, currentEpoch);
                votingStats.setDrepNoConfidenceStake(dRepNoConfidenceStake.orElse(BigInteger.ZERO));

                if (govActionDetail.getType().equals(GovActionType.NO_CONFIDENCE) && dRepNoConfidenceStake.isPresent()) {
                    dRepYesStake = dRepYesStake.add(dRepNoConfidenceStake.get());
                }
                votingStats.setDrepTotalYesStake(dRepYesStake);

                end = System.currentTimeMillis();
                log.debug("GetStakeByDRepTypeAndEpoch (no confidence) time: {} ms", end - start);

                /* Calculate dRep 'no' stake */
                    /*
                        dRepNoStake – The total stake of:
                        1. Registered dReps that voted 'No', plus
                        2. Registered dReps that did not vote for this action, plus
                        3. The AlwaysNoConfidence dRep.
                     */
                dRepNoStake = BigInteger.ZERO;

                start = System.currentTimeMillis();
                List<String> dRepsVoteNo = votesForThisProposalByDRep.stream()
                        .filter(votingProcedure -> votingProcedure.getVote().equals(Vote.NO))
                        .map(DRepUtil::getDRepId)
                        .filter(Objects::nonNull)
                        .toList();
                if (!dRepsVoteNo.isEmpty()) {
                    dRepNoStake = dRepDistStorage.getAllByEpochAndDRepIdsExcludeInactiveDReps(currentEpoch, dRepsVoteNo)
                            .stream()
                            .map(DRepDist::getAmount)
                            .reduce(BigInteger.ZERO, BigInteger::add);
                }
                votingStats.setDrepNoVoteStake(dRepNoStake);

                // The total stake of dReps that voted for this action
                BigInteger totalStakeDRepDoVote = dRepDistStorage.getAllByEpochAndDRepIdsExcludeInactiveDReps(currentEpoch, votesForThisProposalByDRep.stream()
                                .map(DRepUtil::getDRepId)
                                .filter(Objects::nonNull)
                                .toList())
                        .stream()
                        .map(DRepDist::getAmount)
                        .reduce(BigInteger.ZERO, BigInteger::add);

                // The total stake of active dReps that did not vote for this action = totalDRepStake - totalStakeDRepDoVote - dRepAutoAbstainStake - dRepNoConfidenceStake
                BigInteger totalStakeDRepDoNotVote = totalDRepStake.subtract(totalStakeDRepDoVote).subtract(dRepAutoAbstainStake)
                        .subtract(dRepNoConfidenceStake.orElse(BigInteger.ZERO));
                votingStats.setDrepNotVotedStake(totalStakeDRepDoNotVote);

                dRepNoStake = dRepNoStake.add(totalStakeDRepDoNotVote);

                if (dRepNoConfidenceStake.isPresent()) {
                    dRepNoStake = dRepNoStake.add(dRepNoConfidenceStake.get());
                }

                votingStats.setDrepTotalNoStake(dRepNoStake);

                end = System.currentTimeMillis();
                log.debug("GetAllByEpochAndDRepIds (others) time: {} ms", end - start);
            } else {
                    /*
                        During bootstrap phase, For `HardForkInitiation` all SPOs that didn't vote are considered as `No` votes.
                        Whereas, for all other `GovAction`s, SPOs that didn't vote are considered as `Abstain` votes.
                    */
                start = System.currentTimeMillis();
                if (!govActionDetail.getType().equals(GovActionType.HARD_FORK_INITIATION_ACTION)) {
                    List<String> poolsDoNotVoteForThisAction = activePools.stream()
                            .filter(poolId -> votesBySPO.stream()
                                    .noneMatch(votingProcedure -> votingProcedure.getVoterHash().equals(poolId)
                                            && votingProcedure.getGovActionTxHash().equals(proposal.getTxHash())
                                            && votingProcedure.getGovActionIndex() == proposal.getIndex()))
                            .toList();
                    BigInteger totalStakeSPODoNotVote = epochStakeStorage.getAllActiveStakesByEpochAndPools(prevEpoch + 2, poolsDoNotVoteForThisAction)
                            .stream()
                            .map(EpochStake::getAmount)
                            .reduce(BigInteger.ZERO, BigInteger::add);
                    spoAbstainStake = spoAbstainStake.add(totalStakeSPODoNotVote);
                }
                end = System.currentTimeMillis();
                log.debug("GetAllActiveStakesByEpochAndPools (in loop --Bootstrap phase-1) time: {} ms", end - start);
            }

            start = System.currentTimeMillis();
                /*
                    Cases: a pool delegated to an `AlwaysNoConfidence` or an `AlwaysAbstain` DRep.
                    In those cases, behaviour is as expected: vote `Yes` on `NoConfidence` proposals in case of the former
                    and vote `Abstain` by default in case of the latter
                 */
            if (govActionDetail.getType() == GovActionType.NO_CONFIDENCE) {
                spoYesStake = spoYesStake.add(totalStakeSPODelegatedToNoConfidenceDRep);
            }
            votingStats.setSpoTotalYesStake(spoYesStake);

            spoAbstainStake = spoAbstainStake.add(totalStakeSPODelegatedToAbstainDRep);
            votingStats.setSpoTotalAbstainStake(spoAbstainStake);

            BigInteger spoNoStake = totalPoolStake.subtract(spoYesStake).subtract(spoAbstainStake);
            votingStats.setSpoTotalNoStake(spoNoStake);

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
                    .votingStats(votingStats)
                    .epoch(currentEpoch)
                    .status(govActionStatus)
                    .build();

            govActionProposalStatusListNeedToSave.add(govActionProposalStatus);

            if (govActionStatus.equals(GovActionStatus.RATIFIED) && GovernanceActionUtil.isDelayingAction(govActionDetail.getType())) {
                // A successful motion of no-confidence, update of the constitutional committee,
                // a constitutional change, or a hard-fork,
                // delays ratification of all other governance actions until the first epoch after their enactment
                isActionRatificationDelayed = true;
            }

            end = System.currentTimeMillis();
            log.debug("GovActionProposalStatus: Others --, time: {} ms", end - start);

        }
        long loopEnd = System.currentTimeMillis();
        log.debug("GovActionProposalStatus: loop time: {} ms", loopEnd - loopStart);

        return govActionProposalStatusListNeedToSave;
    }

    private void takeDRepDistrSnapshot(int epoch) {
        var start = Instant.now();
        dRepDistService.takeStakeSnapshot(epoch);
        var end = Instant.now();

        Optional<AdaPotJob> adaPotJobOpt = adaPotJobStorage.getJobByTypeAndEpoch(AdaPotJobType.REWARD_CALC, epoch);
        if (adaPotJobOpt.isPresent()) {
            var job = adaPotJobOpt.get();
            job.setDrepDistrSnapshotTime(end.toEpochMilli() - start.toEpochMilli());

            adaPotJobStorage.save(job);
        }
    }

    /**
     * Get proposals for status calculation
     *
     * @param epoch current epoch
     * @return
     */
    private List<GovActionProposal> getProposalsForStatusCalculation(int epoch) {
        List<GovActionProposal> newProposalsCreatedInPrevEpoch = govActionProposalStorage.findByEpoch(epoch - 1)
                .stream()
                .map(govActionProposal -> proposalMapper.toGovActionProposal(govActionProposal))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        List<GovActionProposal> activeProposalsInPrevSnapshot = proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.ACTIVE, epoch - 1);
        List<GovActionProposal> expiredProposalsInPrevSnapshot = proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.EXPIRED, epoch - 1);
        List<GovActionProposal> ratifiedProposalsInPrevSnapshot = proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, epoch - 1);

        List<Proposal> expiredProposals = expiredProposalsInPrevSnapshot.stream()
                .map(proposalMapper::toProposal)
                .toList();

        List<Proposal> ratifiedProposals = ratifiedProposalsInPrevSnapshot.stream()
                .map(proposalMapper::toProposal)
                .toList();

        List<Proposal> activeProposals = activeProposalsInPrevSnapshot.stream()
                .map(proposalMapper::toProposal)
                .toList();

        List<Proposal> allProposals = Stream.concat(expiredProposals.stream(), Stream.concat(ratifiedProposals.stream(), activeProposals.stream())).toList();
        Map<GovActionId, Proposal> siblingsOrDescendantsBeDroppedInCurrentEpoch = new HashMap<>();

        for (Proposal proposal : expiredProposals) {
            List<Proposal> proposalsToPrune = ProposalUtils.findDescendants(proposal, allProposals);
            proposalsToPrune.forEach(p -> siblingsOrDescendantsBeDroppedInCurrentEpoch.put(p.getGovActionId(), p));
        }

        for (Proposal proposal : ratifiedProposals) {
            // TODO: just a workaround, need to check ledger rule carefully.
            if (!GovernanceActionUtil.isDelayingAction(proposal.getType())) {
                List<Proposal> proposalsToPrune = ProposalUtils.findDescendantsAndSiblings(proposal, allProposals);
                proposalsToPrune.forEach(p -> siblingsOrDescendantsBeDroppedInCurrentEpoch.put(p.getGovActionId(), p));
            }
        }

        List<GovActionProposal> filteredActiveProposals = activeProposalsInPrevSnapshot.stream()
                .filter(govActionProposal -> !siblingsOrDescendantsBeDroppedInCurrentEpoch.containsKey(
                        GovActionId.builder()
                                .gov_action_index(govActionProposal.getIndex())
                                .transactionId(govActionProposal.getTxHash())
                                .build()))
                .toList();

        return Stream.concat(filteredActiveProposals.stream(), newProposalsCreatedInPrevEpoch.stream())
                .toList();
    }

    private boolean isEpochInConwayBootstrapPhase(int epoch) {
        boolean result = true;

        if (isPublicNetwork()) {
            Optional<com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam> epochParamOpt = epochParamStorage.getProtocolParams(epoch);

            if (epochParamOpt.isPresent()) {
                var protocolParams = epochParamOpt.get().getParams();
                if (protocolParams.getProtocolMajorVer() >= 10) {
                    result = false;
                }
            }
        } else {
            ProtocolParams genesisProtocolParams = eraGenesisProtocolParamsUtil
                    .getGenesisProtocolParameters(Era.Conway, null, storeProperties.getProtocolMagic())
                    .orElse(null);

            if (genesisProtocolParams != null && genesisProtocolParams.getProtocolMajorVer() >= 10) {
                result = false;
            }
        }

        return result;
    }

    private ProposalVotingStats initProposalVotingStats() {
        return ProposalVotingStats.builder()
            .drepTotalAbstainStake(BigInteger.ZERO)
            .drepTotalYesStake(BigInteger.ZERO)
            .drepTotalNoStake(BigInteger.ZERO)
            .drepNoVoteStake(BigInteger.ZERO)
            .drepNotVotedStake(BigInteger.ZERO)
            .drepNoConfidenceStake(BigInteger.ZERO)
            .spoTotalYesStake(BigInteger.ZERO)
            .spoTotalNoStake(BigInteger.ZERO)
            .spoTotalAbstainStake(BigInteger.ZERO)
            .build();
    }

    private boolean isPublicNetwork() {
        return storeProperties.getProtocolMagic() == Networks.mainnet().getProtocolMagic()
                || storeProperties.getProtocolMagic() == Networks.preprod().getProtocolMagic()
                || storeProperties.getProtocolMagic() == Networks.preview().getProtocolMagic();
    }
}
