package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolDetails;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SPOVotingDataCollector {
    
    private final EpochStakeStorageReader epochStakeStorage;
    private final PoolStorage poolStorage;
    private final PoolStorageReader poolStorageReader;
    private final DelegationVoteDataService delegationVoteDataService;

    public VotingData.SPOVotes collectSPOVotes(GovActionProposal proposal,
                                                List<VotingProcedure> spoVotes,
                                                boolean isInConwayBootstrapPhase,
                                                int epoch) {
        var yesVoteStake = calculateSPOStakeByVote(spoVotes, Vote.YES, epoch);
        var abstainVoteStake = calculateSPOStakeByVote(spoVotes, Vote.ABSTAIN, epoch);
        var totalStake = epochStakeStorage.getTotalActiveStakeByEpoch(epoch + 2).orElse(BigInteger.ZERO);

        List<String> activePools = poolStorage.findActivePools(epoch).stream()
                .map(com.bloxbean.cardano.yaci.store.staking.domain.Pool::getPoolId)
                .toList();

        // map (reward account, List of pools)
        int QUERY_BATCH_SIZE = 500;
        var activePoolsBatches = ListUtil.partition(activePools, QUERY_BATCH_SIZE);
        Map<String, List<String>> rewardAccountPoolMap = activePoolsBatches.parallelStream()
                .flatMap(batch -> poolStorageReader.getPoolDetails(batch, epoch).stream())
                .collect(Collectors.groupingBy(PoolDetails::getRewardAccount, Collectors.mapping(PoolDetails::getPoolId, Collectors.toList())));
        var poolBatches = ListUtil.partition(new ArrayList<>(rewardAccountPoolMap.values()), QUERY_BATCH_SIZE);

        // Calculate the total stake of SPOs that delegated to AlwaysAbstain DRep
        List<String> poolsDelegatedToAlwaysAbstainDRep = new ArrayList<>();
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

        BigInteger totalDoNotVoteStake = BigInteger.ZERO;

        if (isInConwayBootstrapPhase && !proposal.getGovAction().getType().equals(GovActionType.HARD_FORK_INITIATION_ACTION)) {
            List<String> poolsDoNotVoteForThisAction = activePools.stream()
                    .filter(poolId -> spoVotes.stream()
                            .noneMatch(votingProcedure -> votingProcedure.getVoterHash().equals(poolId)
                                    && votingProcedure.getGovActionTxHash().equals(proposal.getTxHash())
                                    && votingProcedure.getGovActionIndex() == proposal.getIndex()))
                    .toList();

            totalDoNotVoteStake = epochStakeStorage.getAllActiveStakesByEpochAndPools(epoch + 2, poolsDoNotVoteForThisAction)
                    .stream()
                    .map(EpochStake::getAmount)
                    .reduce(BigInteger.ZERO, BigInteger::add);
        }

        return VotingData.SPOVotes.builder()
                .yesVoteStake(yesVoteStake)
                .abstainVoteStake(abstainVoteStake)
                .totalStake(totalStake)
                .delegateToAutoAbstainDRepStake(totalStakeSPODelegatedToAbstainDRep)
                .delegateToNoConfidenceDRepStake(totalStakeSPODelegatedToNoConfidenceDRep)
                .doNotVoteStake(totalDoNotVoteStake)
                .build();
    }

    private BigInteger calculateSPOStakeByVote(List<com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure> votes,
                                               Vote voteType, int epoch) {
        var poolIds = votes.stream()
                .filter(vote -> vote.getVote().equals(voteType))
                .map(VotingProcedure::getVoterHash)
                .toList();

        if (poolIds.isEmpty()) {
            return BigInteger.ZERO;
        }

        return epochStakeStorage.getAllActiveStakesByEpochAndPools(epoch + 2, poolIds)
                .stream()
                .map(EpochStake::getAmount)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

}