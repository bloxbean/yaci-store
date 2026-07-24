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
     * Build the pool-level SPO stake snapshot for the supplied epoch.
     *
     * @param epoch epoch for which to compute aggregates
     * @return pool-level stake metrics shared across proposals
     */
    public SPOEpochAggregates buildEpochAggregates(int epoch) {
        // The epoch-stake table stores the snapshot used here under activeEpoch = epoch + 2.
        BigInteger totalActiveStake = epochStakeStorage.getTotalActiveStakeByEpoch(epoch + 2)
                .orElse(BigInteger.ZERO);

        List<String> activePools = poolStorage.findActivePools(epoch).stream()
                .map(com.bloxbean.cardano.yaci.store.staking.domain.Pool::getPoolId)
                .toList();

        var activePoolBatches = ListUtil.partition(activePools, QUERY_BATCH_SIZE);

        // A pool delegates to a default DRep through its reward account, so retain
        // the pool identities while resolving reward-account delegations.
        Map<String, List<String>> rewardAccountToActivePoolsMap = activePoolBatches.parallelStream()
                .flatMap(batch -> poolStorageReader.getPoolDetails(batch, epoch).stream())
                .collect(Collectors.groupingBy(
                        PoolDetails::getRewardAccount,
                        Collectors.mapping(PoolDetails::getPoolId, Collectors.toList())));

        var activeRewardAccountBatches = ListUtil.partition(new ArrayList<>(rewardAccountToActivePoolsMap.keySet()), QUERY_BATCH_SIZE);

        List<String> poolsDelegatedToAlwaysAbstainDRep = activeRewardAccountBatches.parallelStream()
                .flatMap(batch -> delegationVoteDataService
                        .getDelegationVotesByDRepTypeAndAddressList(batch, DrepType.ABSTAIN, epoch)
                        .parallelStream()
                        .flatMap(delegationVote -> rewardAccountToActivePoolsMap
                                .getOrDefault(delegationVote.getAddress(), List.of())
                                .stream()))
                .distinct()
                .toList();

        Map<String, BigInteger> alwaysAbstainStakeByPool = getActiveStakeByPoolBatch(
                epoch + 2, poolsDelegatedToAlwaysAbstainDRep, QUERY_BATCH_SIZE);

        List<String> poolsDelegatedToNoConfidenceDRep = activeRewardAccountBatches.parallelStream()
                .flatMap(batch -> delegationVoteDataService
                        .getDelegationVotesByDRepTypeAndAddressList(batch, DrepType.NO_CONFIDENCE, epoch)
                        .parallelStream()
                        .flatMap(delegationVote -> rewardAccountToActivePoolsMap
                                .getOrDefault(delegationVote.getAddress(), List.of())
                                .stream()))
                .distinct()
                .toList();

        Map<String, BigInteger> alwaysNoConfidenceStakeByPool = getActiveStakeByPoolBatch(
                epoch + 2, poolsDelegatedToNoConfidenceDRep, QUERY_BATCH_SIZE);

        return new SPOEpochAggregates(
                epoch,
                totalActiveStake,
                sumStake(alwaysAbstainStakeByPool),
                sumStake(alwaysNoConfidenceStakeByPool),
                Map.copyOf(alwaysAbstainStakeByPool),
                Map.copyOf(alwaysNoConfidenceStakeByPool));
    }

    /**
     * Build an epoch snapshot without using the batch-global voter list.
     *
     * @param spoVotes ignored because SPO voters are proposal-scoped
     * @param epoch epoch for which to compute aggregates
     * @return pool-level stake metrics shared across proposals
     * @deprecated use {@link #buildEpochAggregates(int)}
     */
    @Deprecated(forRemoval = false)
    public SPOEpochAggregates buildEpochAggregates(List<VotingProcedure> spoVotes, int epoch) {
        return buildEpochAggregates(epoch);
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

        Set<String> poolsThatVotedForProposal = spoVotesForProposal.stream()
                .map(VotingProcedure::getVoterHash)
                .collect(Collectors.toSet());

        // Explicit votes are action-specific: voting on proposal A must not suppress
        // the pool's default on proposal B. cardano-ledger's spoAcceptedRatio applies
        // defaults only after checking the current action's gasStakePoolVotes map.
        BigInteger delegateToAutoAbstainDRepStake = defaultStakeForNonVoters(
                spoEpochAggregates.alwaysAbstainStakeByPool(),
                spoEpochAggregates.delegateToAutoAbstainDRepStake(),
                poolsThatVotedForProposal);
        BigInteger delegateToNoConfidenceDRepStake = defaultStakeForNonVoters(
                spoEpochAggregates.alwaysNoConfidenceStakeByPool(),
                spoEpochAggregates.delegateToNoConfidenceDRepStake(),
                poolsThatVotedForProposal);

        BigInteger totalDoNotVoteStake = spoEpochAggregates.totalStake()
                .subtract(yesVoteStake)
                .subtract(noVoteStake)
                .subtract(abstainVoteStake)
                .subtract(delegateToAutoAbstainDRepStake)
                .subtract(delegateToNoConfidenceDRepStake);

        return AggregatedVotingData.SPOVotes.builder()
                .yesVoteStake(yesVoteStake)
                .abstainVoteStake(abstainVoteStake)
                .noVoteStake(noVoteStake)
                .totalStake(spoEpochAggregates.totalStake())
                .delegateToAutoAbstainDRepStake(delegateToAutoAbstainDRepStake)
                .delegateToNoConfidenceDRepStake(delegateToNoConfidenceDRepStake)
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

    private BigInteger defaultStakeForNonVoters(Map<String, BigInteger> stakeByPool,
                                                BigInteger aggregateStake,
                                                Set<String> poolsThatVotedForProposal) {
        if (stakeByPool.isEmpty()) {
            return aggregateStake;
        }

        return stakeByPool.entrySet().stream()
                .filter(entry -> !poolsThatVotedForProposal.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private BigInteger sumStake(Map<String, BigInteger> stakeByPool) {
        return stakeByPool.values().stream()
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private Map<String, BigInteger> getActiveStakeByPoolBatch(int activeEpoch, List<String> poolIds, int batchSize) {
        if (poolIds.isEmpty()) {
            return Map.of();
        }

        return ListUtil.partition(poolIds, batchSize)
                .parallelStream()
                .flatMap(batch -> epochStakeStorage.getAllActiveStakesByEpochAndPools(activeEpoch, batch).stream())
                .collect(Collectors.toMap(
                        EpochStake::getPoolId,
                        EpochStake::getAmount,
                        BigInteger::add));
    }

    // Aggregate fields retain the original accessors; per-pool maps allow each
    // proposal to exclude only the pools that cast an explicit vote on it.
    public record SPOEpochAggregates(int epoch,
                                     BigInteger totalStake,
                                     BigInteger delegateToAutoAbstainDRepStake,
                                     BigInteger delegateToNoConfidenceDRepStake,
                                     Map<String, BigInteger> alwaysAbstainStakeByPool,
                                     Map<String, BigInteger> alwaysNoConfidenceStakeByPool) {

        public SPOEpochAggregates(int epoch,
                                  BigInteger totalStake,
                                  BigInteger delegateToAutoAbstainDRepStake,
                                  BigInteger delegateToNoConfidenceDRepStake) {
            this(
                    epoch,
                    totalStake,
                    delegateToAutoAbstainDRepStake,
                    delegateToNoConfidenceDRepStake,
                    Map.of(),
                    Map.of());
        }
    }
}
