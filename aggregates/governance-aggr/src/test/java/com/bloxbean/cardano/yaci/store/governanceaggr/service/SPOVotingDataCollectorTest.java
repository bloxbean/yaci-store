package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.actions.NoConfidence;
import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil;
import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.AggregatedVotingData;
import com.bloxbean.cardano.yaci.store.governancerules.api.VotingData;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingEvaluationContext;
import com.bloxbean.cardano.yaci.store.governancerules.voting.VotingStatus;
import com.bloxbean.cardano.yaci.store.governancerules.voting.spo.SPOVotingEvaluator;
import com.bloxbean.cardano.yaci.store.staking.domain.Pool;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolDetails;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorageReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SPOVotingDataCollectorTest {

    private static final int SNAPSHOT_EPOCH = 100;
    private static final int ACTIVE_EPOCH = SNAPSHOT_EPOCH + 2;
    private static final String POOL_WITH_DEFAULT = "pool-with-default";
    private static final String EXPLICIT_VOTER = "explicit-voter";
    private static final String REWARD_ACCOUNT = "stake_test1_default";
    private static final String SHARED_ACCOUNT_POOL_A = "shared-account-pool-a";
    private static final String SHARED_ACCOUNT_POOL_B = "shared-account-pool-b";

    @Mock
    private EpochStakeStorageReader epochStakeStorage;
    @Mock
    private PoolStorage poolStorage;
    @Mock
    private PoolStorageReader poolStorageReader;
    @Mock
    private DelegationVoteDataService delegationVoteDataService;

    @Test
    void collectSPOVotes_shouldApplyAlwaysAbstainPerProposal() {
        configureEpochSnapshot(DrepType.ABSTAIN);
        var collector = new SPOVotingDataCollector(epochStakeStorage, poolStorage, poolStorageReader, delegationVoteDataService);
        var voteOnProposalA = vote(POOL_WITH_DEFAULT, "proposal-a", Vote.YES);
        var voteOnProposalB = vote(EXPLICIT_VOTER, "proposal-b", Vote.YES);

        // The default pool voted on A, but it did not vote on B. Its stake must
        // therefore still enter B through the AlwaysAbstain default.
        var epochAggregates = collector.buildEpochAggregates(SNAPSHOT_EPOCH);
        var proposalAVotes = collector.collectSPOVotes(List.of(voteOnProposalA), epochAggregates);
        var proposalBVotes = collector.collectSPOVotes(List.of(voteOnProposalB), epochAggregates);

        assertThat(epochAggregates.delegateToAutoAbstainDRepStake()).isEqualTo(BigInteger.valueOf(100));
        assertThat(proposalAVotes.getDelegateToAutoAbstainDRepStake()).isZero();
        assertThat(proposalBVotes.getYesVoteStake()).isEqualTo(BigInteger.valueOf(100));
        assertThat(proposalBVotes.getDelegateToAutoAbstainDRepStake()).isEqualTo(BigInteger.valueOf(100));
        assertThat(proposalBVotes.getDelegateToNoConfidenceDRepStake()).isZero();
        assertThat(proposalBVotes.getDoNotVoteStake()).isZero();
    }

    @Test
    void collectSPOVotes_shouldApplyAlwaysNoConfidencePerProposal() {
        configureEpochSnapshot(DrepType.NO_CONFIDENCE);
        var collector = new SPOVotingDataCollector(epochStakeStorage, poolStorage, poolStorageReader, delegationVoteDataService);
        var voteOnProposalA = vote(POOL_WITH_DEFAULT, "proposal-a", Vote.ABSTAIN);
        var voteOnProposalB = vote(EXPLICIT_VOTER, "proposal-b", Vote.YES);

        // As above, an explicit vote on A must not hide the pool's
        // AlwaysNoConfidence default from B.
        var epochAggregates = collector.buildEpochAggregates(SNAPSHOT_EPOCH);
        var proposalAVotes = collector.collectSPOVotes(List.of(voteOnProposalA), epochAggregates);
        var proposalBVotes = collector.collectSPOVotes(List.of(voteOnProposalB), epochAggregates);

        assertThat(proposalAVotes.getDelegateToNoConfidenceDRepStake()).isZero();
        assertThat(proposalBVotes.getYesVoteStake()).isEqualTo(BigInteger.valueOf(100));
        assertThat(proposalBVotes.getDelegateToAutoAbstainDRepStake()).isZero();
        assertThat(proposalBVotes.getDelegateToNoConfidenceDRepStake()).isEqualTo(BigInteger.valueOf(100));
        assertThat(proposalBVotes.getDoNotVoteStake()).isZero();
    }

    @Test
    void collectSPOVotes_shouldLetExplicitVoteOverrideDefaultForSameProposal() {
        configureEpochSnapshot(DrepType.ABSTAIN);
        var collector = new SPOVotingDataCollector(epochStakeStorage, poolStorage, poolStorageReader, delegationVoteDataService);
        var explicitVote = vote(POOL_WITH_DEFAULT, "proposal-b", Vote.YES);

        // On the same proposal, explicit YES replaces the pool's default; the
        // stake must not be counted a second time as AlwaysAbstain.
        var epochAggregates = collector.buildEpochAggregates(SNAPSHOT_EPOCH);
        var proposalVotes = collector.collectSPOVotes(List.of(explicitVote), epochAggregates);

        assertThat(proposalVotes.getYesVoteStake()).isEqualTo(BigInteger.valueOf(100));
        assertThat(proposalVotes.getDelegateToAutoAbstainDRepStake()).isZero();
        assertThat(proposalVotes.getDelegateToNoConfidenceDRepStake()).isZero();
        assertThat(proposalVotes.getDoNotVoteStake()).isEqualTo(BigInteger.valueOf(100));
    }

    @Test
    void collectSPOVotes_shouldIncludeMultipleProposalDepositsInExplicitAndDefaultStake() {
        configureEpochSnapshot(DrepType.ABSTAIN);
        var collector = new SPOVotingDataCollector(epochStakeStorage, poolStorage, poolStorageReader, delegationVoteDataService);

        when(epochStakeStorage.getAllActiveStakesByAddressesAndEpoch(anyList(), eq(ACTIVE_EPOCH)))
                .thenReturn(List.of(
                        epochStake(EXPLICIT_VOTER, "stake_test1_explicit_1", 60),
                        epochStake(EXPLICIT_VOTER, "stake_test1_explicit_2", 40),
                        epochStake(POOL_WITH_DEFAULT, REWARD_ACCOUNT, 100)));

        var activeProposals = List.of(
                proposal("stake_test1_explicit_1", 10),
                proposal("stake_test1_explicit_2", 20),
                proposal(REWARD_ACCOUNT, 30));

        var epochAggregates = collector.buildEpochAggregates(SNAPSHOT_EPOCH, activeProposals);
        var proposalVotes = collector.collectSPOVotes(
                List.of(vote(EXPLICIT_VOTER, "proposal", Vote.YES)), epochAggregates);

        assertThat(epochAggregates.proposalDepositByPool()).containsExactlyInAnyOrderEntriesOf(Map.of(
                EXPLICIT_VOTER, BigInteger.valueOf(30),
                POOL_WITH_DEFAULT, BigInteger.valueOf(30)));
        assertThat(proposalVotes.getTotalStake()).isEqualTo(BigInteger.valueOf(260));
        assertThat(proposalVotes.getYesVoteStake()).isEqualTo(BigInteger.valueOf(130));
        assertThat(proposalVotes.getDelegateToAutoAbstainDRepStake()).isEqualTo(BigInteger.valueOf(130));
        assertThat(proposalVotes.getDoNotVoteStake()).isZero();
    }

    @Test
    void collectSPOVotes_shouldApplyOneRewardAccountDefaultToEveryPoolUsingIt() {
        configureSharedRewardAccountSnapshot(true);
        var collector = new SPOVotingDataCollector(epochStakeStorage, poolStorage, poolStorageReader, delegationVoteDataService);

        // Both pools declare the same reward account, so its AlwaysAbstain delegation applies twice.
        var epochAggregates = collector.buildEpochAggregates(SNAPSHOT_EPOCH);
        var proposalVotes = collector.collectSPOVotes(List.of(), epochAggregates);

        assertThat(proposalVotes.getDelegateToAutoAbstainDRepStake()).isEqualTo(BigInteger.valueOf(200));
        assertThat(proposalVotes.getDoNotVoteStake()).isZero();
    }

    @Test
    void buildEpochAggregates_shouldExcludeIneligibleDepositsAndNotRetainThemOnReplay() {
        var eligiblePool = "eligible-pool";
        when(epochStakeStorage.getTotalActiveStakeByEpoch(ACTIVE_EPOCH))
                .thenReturn(Optional.of(BigInteger.valueOf(100)));
        when(poolStorage.findActivePools(SNAPSHOT_EPOCH)).thenReturn(List.of());
        when(epochStakeStorage.getAllActiveStakesByAddressesAndEpoch(anyList(), eq(ACTIVE_EPOCH)))
                .thenReturn(List.of(
                        epochStake(eligiblePool, "stake_test1_eligible_1", 60),
                        epochStake(eligiblePool, "stake_test1_eligible_2", 40)));
        when(epochStakeStorage.getAllActiveStakesByEpochAndPools(eq(ACTIVE_EPOCH), anyList()))
                .thenAnswer(invocation -> {
                    List<String> poolIds = invocation.getArgument(1);
                    return List.of(
                                    epochStake(eligiblePool, "stake_test1_eligible_1", 60),
                                    epochStake(eligiblePool, "stake_test1_eligible_2", 40))
                            .stream()
                            .filter(stake -> poolIds.contains(stake.getPoolId()))
                            .toList();
                });

        var collector = new SPOVotingDataCollector(epochStakeStorage, poolStorage, poolStorageReader, delegationVoteDataService);
        var epochAggregates = collector.buildEpochAggregates(SNAPSHOT_EPOCH, List.of(
                proposal("stake_test1_eligible_1", 10),
                proposal("stake_test1_eligible_2", 15),
                proposal("stake_test1_no_pool_delegation", 20),
                proposal("stake_test1_deregistered", 25),
                proposal("stake_test1_absent_pool", 30)));

        assertThat(epochAggregates.totalStake()).isEqualTo(BigInteger.valueOf(125));
        assertThat(epochAggregates.proposalDepositByPool())
                .containsExactly(Map.entry(eligiblePool, BigInteger.valueOf(25)));

        var proposalVotes = collector.collectSPOVotes(
                List.of(vote(eligiblePool, "proposal", Vote.YES)), epochAggregates);
        assertThat(proposalVotes.getYesVoteStake()).isEqualTo(BigInteger.valueOf(125));
        assertThat(proposalVotes.getDoNotVoteStake()).isZero();

        // A replay after the proposal leaves the active set recomputes from input and
        // must not retain or duplicate its deposit from the prior snapshot.
        var replayedAggregates = collector.buildEpochAggregates(SNAPSHOT_EPOCH, List.of());
        assertThat(replayedAggregates.totalStake()).isEqualTo(BigInteger.valueOf(100));
        assertThat(replayedAggregates.proposalDepositByPool()).isEmpty();
    }

    @Test
    void collectSPOVotes_shouldCrossRatificationThresholdWhenEligibleDepositBacksYesPool() {
        var yesPool = "yes-pool";
        var returnAccount = "stake_test1_near_threshold";
        when(epochStakeStorage.getTotalActiveStakeByEpoch(ACTIVE_EPOCH))
                .thenReturn(Optional.of(BigInteger.valueOf(200)));
        when(poolStorage.findActivePools(SNAPSHOT_EPOCH)).thenReturn(List.of());
        when(epochStakeStorage.getAllActiveStakesByAddressesAndEpoch(anyList(), eq(ACTIVE_EPOCH)))
                .thenReturn(List.of(epochStake(yesPool, returnAccount, 100)));
        when(epochStakeStorage.getAllActiveStakesByEpochAndPools(eq(ACTIVE_EPOCH), anyList()))
                .thenReturn(List.of(epochStake(yesPool, returnAccount, 100)));

        var collector = new SPOVotingDataCollector(epochStakeStorage, poolStorage, poolStorageReader, delegationVoteDataService);
        var yesVote = List.of(vote(yesPool, "proposal", Vote.YES));

        var withoutDeposit = collector.collectSPOVotes(
                yesVote, collector.buildEpochAggregates(SNAPSHOT_EPOCH, List.of()));
        var withDeposit = collector.collectSPOVotes(
                yesVote,
                collector.buildEpochAggregates(
                        SNAPSHOT_EPOCH, List.of(proposal(returnAccount, 51))));

        var noConfidence = mock(NoConfidence.class);
        when(noConfidence.getType()).thenReturn(GovActionType.NO_CONFIDENCE);
        var votingContext = VotingEvaluationContext.builder()
                .govAction(noConfidence)
                .poolThresholds(PoolVotingThresholds.builder()
                        .pvtMotionNoConfidence(UnitIntervalUtil.decimalToUnitInterval(new BigDecimal("0.60")))
                        .build())
                .build();
        var evaluator = new SPOVotingEvaluator();

        assertThat(evaluator.evaluate(toVotingData(withoutDeposit), votingContext))
                .isEqualTo(VotingStatus.NOT_PASS_THRESHOLD);
        assertThat(evaluator.evaluate(toVotingData(withDeposit), votingContext))
                .isEqualTo(VotingStatus.PASS_THRESHOLD);
    }

    @Test
    void collectSPOVotes_shouldTreatSharedRewardAccountWithoutEffectiveDelegationAsNonDelegating() {
        configureSharedRewardAccountSnapshot(false);
        var collector = new SPOVotingDataCollector(epochStakeStorage, poolStorage, poolStorageReader, delegationVoteDataService);

        // Once the shared reward account no longer has an effective delegation, for example after
        // deregistration, neither pool may keep the AlwaysAbstain default.
        var epochAggregates = collector.buildEpochAggregates(SNAPSHOT_EPOCH);
        var proposalVotes = collector.collectSPOVotes(List.of(), epochAggregates);

        assertThat(proposalVotes.getDelegateToAutoAbstainDRepStake()).isZero();
        assertThat(proposalVotes.getDelegateToNoConfidenceDRepStake()).isZero();
        assertThat(proposalVotes.getDoNotVoteStake()).isEqualTo(BigInteger.valueOf(200));
    }

    private void configureSharedRewardAccountSnapshot(boolean rewardAccountHasEffectiveAbstainDelegation) {
        var firstPool = Pool.builder().poolId(SHARED_ACCOUNT_POOL_A).build();
        var secondPool = Pool.builder().poolId(SHARED_ACCOUNT_POOL_B).build();
        var firstPoolDetails = PoolDetails.builder()
                .poolId(SHARED_ACCOUNT_POOL_A)
                .rewardAccount(REWARD_ACCOUNT)
                .build();
        var secondPoolDetails = PoolDetails.builder()
                .poolId(SHARED_ACCOUNT_POOL_B)
                .rewardAccount(REWARD_ACCOUNT)
                .build();

        when(epochStakeStorage.getTotalActiveStakeByEpoch(ACTIVE_EPOCH))
                .thenReturn(Optional.of(BigInteger.valueOf(200)));
        when(poolStorage.findActivePools(SNAPSHOT_EPOCH))
                .thenReturn(List.of(firstPool, secondPool));
        when(poolStorageReader.getPoolDetails(anyList(), eq(SNAPSHOT_EPOCH)))
                .thenReturn(List.of(firstPoolDetails, secondPoolDetails));
        when(delegationVoteDataService.getDelegationVotesByDRepTypeAndAddressList(anyList(), eq(DrepType.ABSTAIN), eq(SNAPSHOT_EPOCH)))
                .thenReturn(rewardAccountHasEffectiveAbstainDelegation
                        ? List.of(DelegationVote.builder().address(REWARD_ACCOUNT).drepType(DrepType.ABSTAIN).build())
                        : List.of());
        when(delegationVoteDataService.getDelegationVotesByDRepTypeAndAddressList(anyList(), eq(DrepType.NO_CONFIDENCE), eq(SNAPSHOT_EPOCH)))
                .thenReturn(List.of());

        if (rewardAccountHasEffectiveAbstainDelegation) {
            when(epochStakeStorage.getAllActiveStakesByEpochAndPools(eq(ACTIVE_EPOCH), anyList()))
                    .thenAnswer(invocation -> {
                        List<String> poolIds = invocation.getArgument(1);
                        return poolIds.stream().map(poolId -> epochStake(poolId, 100)).toList();
                    });
        }
    }

    private void configureEpochSnapshot(DrepType defaultType) {
        var defaultPool = Pool.builder().poolId(POOL_WITH_DEFAULT).build();
        var explicitPool = Pool.builder().poolId(EXPLICIT_VOTER).build();
        var defaultPoolDetails = PoolDetails.builder()
                .poolId(POOL_WITH_DEFAULT)
                .rewardAccount(REWARD_ACCOUNT)
                .build();
        var explicitPoolDetails = PoolDetails.builder()
                .poolId(EXPLICIT_VOTER)
                .rewardAccount("stake_test1_explicit")
                .build();
        var delegationVote = DelegationVote.builder()
                .address(REWARD_ACCOUNT)
                .drepType(defaultType)
                .build();

        when(epochStakeStorage.getTotalActiveStakeByEpoch(ACTIVE_EPOCH))
                .thenReturn(Optional.of(BigInteger.valueOf(200)));
        when(poolStorage.findActivePools(SNAPSHOT_EPOCH))
                .thenReturn(List.of(defaultPool, explicitPool));
        when(poolStorageReader.getPoolDetails(anyList(), eq(SNAPSHOT_EPOCH)))
                .thenReturn(List.of(defaultPoolDetails, explicitPoolDetails));
        when(delegationVoteDataService.getDelegationVotesByDRepTypeAndAddressList(anyList(), eq(DrepType.ABSTAIN), eq(SNAPSHOT_EPOCH)))
                .thenReturn(defaultType == DrepType.ABSTAIN ? List.of(delegationVote) : List.of());
        when(delegationVoteDataService.getDelegationVotesByDRepTypeAndAddressList(anyList(), eq(DrepType.NO_CONFIDENCE), eq(SNAPSHOT_EPOCH)))
                .thenReturn(defaultType == DrepType.NO_CONFIDENCE ? List.of(delegationVote) : List.of());
        when(epochStakeStorage.getAllActiveStakesByEpochAndPools(eq(ACTIVE_EPOCH), anyList()))
                .thenAnswer(invocation -> {
                    List<String> poolIds = invocation.getArgument(1);
                    // The default pool uses two rows to verify that all stake rows
                    // for the same pool are combined before applying its default.
                    return poolIds.stream()
                            .flatMap(poolId -> {
                                if (poolId.equals(POOL_WITH_DEFAULT)) {
                                    return List.of(epochStake(poolId, 40), epochStake(poolId, 60)).stream();
                                }
                                return List.of(epochStake(poolId, 100)).stream();
                            })
                            .toList();
                });
    }

    private EpochStake epochStake(String poolId, long amount) {
        return epochStake(poolId, null, amount);
    }

    private EpochStake epochStake(String poolId, String address, long amount) {
        return EpochStake.builder()
                .activeEpoch(ACTIVE_EPOCH)
                .poolId(poolId)
                .address(address)
                .amount(BigInteger.valueOf(amount))
                .build();
    }

    private GovActionProposal proposal(String returnAddress, long deposit) {
        return GovActionProposal.builder()
                .returnAddress(returnAddress)
                .deposit(BigInteger.valueOf(deposit))
                .build();
    }

    private VotingData toVotingData(AggregatedVotingData.SPOVotes spoVotes) {
        return VotingData.builder()
                .spoVotes(VotingData.SPOVotes.builder()
                        .yesVoteStake(spoVotes.getYesVoteStake())
                        .delegateToAutoAbstainDRepStake(spoVotes.getDelegateToAutoAbstainDRepStake())
                        .delegateToNoConfidenceDRepStake(spoVotes.getDelegateToNoConfidenceDRepStake())
                        .abstainVoteStake(spoVotes.getAbstainVoteStake())
                        .doNotVoteStake(spoVotes.getDoNotVoteStake())
                        .totalStake(spoVotes.getTotalStake())
                        .build())
                .build();
    }

    private VotingProcedure vote(String poolId, String proposalId, Vote vote) {
        return VotingProcedure.builder()
                .voterHash(poolId)
                .govActionTxHash(proposalId)
                .govActionIndex(0)
                .vote(vote)
                .build();
    }
}
