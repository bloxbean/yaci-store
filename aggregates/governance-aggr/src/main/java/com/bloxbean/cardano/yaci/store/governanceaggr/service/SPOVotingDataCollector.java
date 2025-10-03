package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.AggregatedVotingData;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
// Pre-computes SPO stake aggregates and per-proposal vote totals each epoch
public class SPOVotingDataCollector {

    private static final int QUERY_BATCH_SIZE = 500;

    private final EpochStakeStorageReader epochStakeStorage;
    private final PoolStorage poolStorage;
    private final PoolStorageReader poolStorageReader;
    private final DelegationVoteDataService delegationVoteDataService;

    /**
     * Build SPO-level stake aggregates for the supplied epoch.
     *
     * @param spoVotes votes cast by SPOs across all proposals in scope
     * @param epoch    epoch for which to compute aggregates
     * @return aggregated SPO stake metrics shared across proposals
     */
    public SPOEpochAggregates buildEpochAggregates(List<VotingProcedure> spoVotes, int epoch) {
        BigInteger totalActiveStake = epochStakeStorage.getTotalActiveStakeByEpoch(epoch + 2)
                .orElse(BigInteger.ZERO);

        List<String> activePools = poolStorage.findActivePools(epoch).stream()
                .map(com.bloxbean.cardano.yaci.store.staking.domain.Pool::getPoolId)
                .toList();

        Set<String> poolsThatVoted = spoVotes.stream()
                .map(VotingProcedure::getVoterHash)
                .collect(Collectors.toSet());

        List<String> poolsWithNonVotingSPOs = activePools.stream()
                .filter(poolId -> !poolsThatVoted.contains(poolId))
                .toList();

        var nonVotingSPOPoolBatches = ListUtil.partition(poolsWithNonVotingSPOs, QUERY_BATCH_SIZE);

        Map<String, List<String>> rewardAccountToNonVotingPoolsMap = nonVotingSPOPoolBatches.parallelStream()
                .flatMap(batch -> poolStorageReader.getPoolDetails(batch, epoch).stream())
                .collect(Collectors.groupingBy(
                        PoolDetails::getRewardAccount,
                        Collectors.mapping(PoolDetails::getPoolId, Collectors.toList())));

        var nonVotingSPORewardAccountBatches = ListUtil.partition(new ArrayList<>(rewardAccountToNonVotingPoolsMap.keySet()), QUERY_BATCH_SIZE);

        List<String> poolsDelegatedToAlwaysAbstainDRep = nonVotingSPORewardAccountBatches.parallelStream()
                .flatMap(batch -> delegationVoteDataService
                        .getDelegationVotesByDRepTypeAndAddressList(batch, DrepType.ABSTAIN, epoch)
                        .parallelStream()
                        .flatMap(delegationVote -> rewardAccountToNonVotingPoolsMap
                                .getOrDefault(delegationVote.getAddress(), List.of())
                                .stream()))
                .collect(Collectors.toList());

        BigInteger totalStakeSPODelegatedToAbstainDRep = getActiveStakesByEpochAndPoolsBatch(epoch + 2, poolsDelegatedToAlwaysAbstainDRep, QUERY_BATCH_SIZE);

        List<String> poolsDelegatedToNoConfidenceDRep = nonVotingSPORewardAccountBatches.parallelStream()
                .flatMap(batch -> delegationVoteDataService
                        .getDelegationVotesByDRepTypeAndAddressList(batch, DrepType.NO_CONFIDENCE, epoch)
                        .parallelStream()
                        .flatMap(delegationVote -> rewardAccountToNonVotingPoolsMap
                                .getOrDefault(delegationVote.getAddress(), List.of())
                                .stream()))
                .collect(Collectors.toList());

        BigInteger totalStakeSPODelegatedToNoConfidenceDRep = getActiveStakesByEpochAndPoolsBatch(epoch + 2, poolsDelegatedToNoConfidenceDRep, QUERY_BATCH_SIZE);

        return new SPOEpochAggregates(epoch, totalActiveStake, totalStakeSPODelegatedToAbstainDRep, totalStakeSPODelegatedToNoConfidenceDRep);
    }

    /**
     * Aggregate SPO aggregated voting data for a specific proposal
     *
     * @param spoVotesForProposal votes emitted by SPOs for the proposal
     * @param spoEpochAggregates  epoch-level SPO stake aggregates
     * @return aggregated SPO voting data for the proposal
     */
    public AggregatedVotingData.SPOVotes collectSPOVotes(List<VotingProcedure> spoVotesForProposal, SPOEpochAggregates spoEpochAggregates) {
        var yesVoteStake = calculateSPOStakeByVote(spoVotesForProposal, Vote.YES, spoEpochAggregates.epoch());
        var abstainVoteStake = calculateSPOStakeByVote(spoVotesForProposal, Vote.ABSTAIN, spoEpochAggregates.epoch());
        var noVoteStake = calculateSPOStakeByVote(spoVotesForProposal, Vote.NO, spoEpochAggregates.epoch());

        BigInteger totalDoNotVoteStake = spoEpochAggregates.totalStake()
                .subtract(yesVoteStake)
                .subtract(noVoteStake)
                .subtract(abstainVoteStake)
                .subtract(spoEpochAggregates.delegateToAutoAbstainDRepStake())
                .subtract(spoEpochAggregates.delegateToNoConfidenceDRepStake());

        return AggregatedVotingData.SPOVotes.builder()
                .yesVoteStake(yesVoteStake)
                .abstainVoteStake(abstainVoteStake)
                .noVoteStake(noVoteStake)
                .totalStake(spoEpochAggregates.totalStake())
                .delegateToAutoAbstainDRepStake(spoEpochAggregates.delegateToAutoAbstainDRepStake())
                .delegateToNoConfidenceDRepStake(spoEpochAggregates.delegateToNoConfidenceDRepStake())
                .doNotVoteStake(totalDoNotVoteStake)
                .build();
    }

    private BigInteger calculateSPOStakeByVote(List<VotingProcedure> votes, Vote voteType, int epoch) {
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

    private BigInteger getActiveStakesByEpochAndPoolsBatch(int activeEpoch, List<String> poolIds, int batchSize) {
        if (poolIds.isEmpty()) {
            return BigInteger.ZERO;
        }

        return ListUtil.partition(poolIds, batchSize)
                .parallelStream()
                .flatMap(batch -> epochStakeStorage.getAllActiveStakesByEpochAndPools(activeEpoch, batch).stream())
                .map(EpochStake::getAmount)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    public record SPOEpochAggregates(int epoch,
                                     BigInteger totalStake,
                                     BigInteger delegateToAutoAbstainDRepStake,
                                     BigInteger delegateToNoConfidenceDRepStake) {
    }
}
