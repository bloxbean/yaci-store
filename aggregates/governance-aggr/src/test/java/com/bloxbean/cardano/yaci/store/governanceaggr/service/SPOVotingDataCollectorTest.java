package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.staking.domain.Pool;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolDetails;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorageReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SPOVotingDataCollectorTest {

    private static final int SNAPSHOT_EPOCH = 100;
    private static final int ACTIVE_EPOCH = SNAPSHOT_EPOCH + 2;
    private static final String POOL_WITH_DEFAULT = "pool-with-default";
    private static final String EXPLICIT_VOTER = "explicit-voter";
    private static final String REWARD_ACCOUNT = "stake_test1_default";

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
        return EpochStake.builder()
                .activeEpoch(ACTIVE_EPOCH)
                .poolId(poolId)
                .amount(BigInteger.valueOf(amount))
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
