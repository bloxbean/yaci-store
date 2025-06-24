package com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.domain.InstantReward;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.domain.UnclaimedRewardRest;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.EpochStakeEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.InstantRewardEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardRestEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.UnclaimedRewardRestEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:27+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class MapperImpl extends Mapper {

    @Override
    public InstantReward toInstantReward(InstantRewardEntity rewardEntity) {
        if ( rewardEntity == null ) {
            return null;
        }

        InstantReward.InstantRewardBuilder<?, ?> instantReward = InstantReward.builder();

        instantReward.address( rewardEntity.getAddress() );
        instantReward.amount( rewardEntity.getAmount() );
        instantReward.earnedEpoch( rewardEntity.getEarnedEpoch() );
        instantReward.slot( rewardEntity.getSlot() );
        instantReward.spendableEpoch( rewardEntity.getSpendableEpoch() );
        instantReward.type( rewardEntity.getType() );

        return instantReward.build();
    }

    @Override
    public InstantRewardEntity toInstantRewardEntity(InstantReward reward) {
        if ( reward == null ) {
            return null;
        }

        InstantRewardEntity.InstantRewardEntityBuilder<?, ?> instantRewardEntity = InstantRewardEntity.builder();

        instantRewardEntity.address( reward.getAddress() );
        instantRewardEntity.amount( reward.getAmount() );
        instantRewardEntity.earnedEpoch( reward.getEarnedEpoch() );
        instantRewardEntity.slot( reward.getSlot() );
        instantRewardEntity.spendableEpoch( reward.getSpendableEpoch() );
        instantRewardEntity.type( reward.getType() );

        return instantRewardEntity.build();
    }

    @Override
    public RewardRest toRewardRest(RewardRestEntity rewardRestEntity) {
        if ( rewardRestEntity == null ) {
            return null;
        }

        RewardRest.RewardRestBuilder<?, ?> rewardRest = RewardRest.builder();

        rewardRest.address( rewardRestEntity.getAddress() );
        rewardRest.amount( rewardRestEntity.getAmount() );
        rewardRest.earnedEpoch( rewardRestEntity.getEarnedEpoch() );
        rewardRest.slot( rewardRestEntity.getSlot() );
        rewardRest.spendableEpoch( rewardRestEntity.getSpendableEpoch() );
        rewardRest.type( rewardRestEntity.getType() );

        return rewardRest.build();
    }

    @Override
    public RewardRestEntity toRewardRestEntity(RewardRest rewardRest) {
        if ( rewardRest == null ) {
            return null;
        }

        RewardRestEntity.RewardRestEntityBuilder<?, ?> rewardRestEntity = RewardRestEntity.builder();

        rewardRestEntity.address( rewardRest.getAddress() );
        rewardRestEntity.amount( rewardRest.getAmount() );
        rewardRestEntity.earnedEpoch( rewardRest.getEarnedEpoch() );
        rewardRestEntity.slot( rewardRest.getSlot() );
        rewardRestEntity.spendableEpoch( rewardRest.getSpendableEpoch() );
        rewardRestEntity.type( rewardRest.getType() );

        return rewardRestEntity.build();
    }

    @Override
    public Reward toReward(RewardEntity rewardEntity) {
        if ( rewardEntity == null ) {
            return null;
        }

        Reward.RewardBuilder<?, ?> reward = Reward.builder();

        reward.address( rewardEntity.getAddress() );
        reward.amount( rewardEntity.getAmount() );
        reward.earnedEpoch( rewardEntity.getEarnedEpoch() );
        reward.poolId( rewardEntity.getPoolId() );
        reward.slot( rewardEntity.getSlot() );
        reward.spendableEpoch( rewardEntity.getSpendableEpoch() );
        reward.type( rewardEntity.getType() );

        return reward.build();
    }

    @Override
    public RewardEntity toRewardEntity(Reward reward) {
        if ( reward == null ) {
            return null;
        }

        RewardEntity.RewardEntityBuilder<?, ?> rewardEntity = RewardEntity.builder();

        rewardEntity.address( reward.getAddress() );
        rewardEntity.amount( reward.getAmount() );
        rewardEntity.earnedEpoch( reward.getEarnedEpoch() );
        rewardEntity.poolId( reward.getPoolId() );
        rewardEntity.slot( reward.getSlot() );
        rewardEntity.spendableEpoch( reward.getSpendableEpoch() );
        rewardEntity.type( reward.getType() );

        return rewardEntity.build();
    }

    @Override
    public UnclaimedRewardRest toUnclaimedRewardRest(UnclaimedRewardRestEntity unclaimedRewardRestEntity) {
        if ( unclaimedRewardRestEntity == null ) {
            return null;
        }

        UnclaimedRewardRest.UnclaimedRewardRestBuilder<?, ?> unclaimedRewardRest = UnclaimedRewardRest.builder();

        unclaimedRewardRest.address( unclaimedRewardRestEntity.getAddress() );
        unclaimedRewardRest.amount( unclaimedRewardRestEntity.getAmount() );
        unclaimedRewardRest.earnedEpoch( unclaimedRewardRestEntity.getEarnedEpoch() );
        unclaimedRewardRest.slot( unclaimedRewardRestEntity.getSlot() );
        unclaimedRewardRest.spendableEpoch( unclaimedRewardRestEntity.getSpendableEpoch() );
        unclaimedRewardRest.type( unclaimedRewardRestEntity.getType() );

        return unclaimedRewardRest.build();
    }

    @Override
    public UnclaimedRewardRestEntity toUnclaimedRewardRestEntity(UnclaimedRewardRest unclaimedRewardRest) {
        if ( unclaimedRewardRest == null ) {
            return null;
        }

        UnclaimedRewardRestEntity.UnclaimedRewardRestEntityBuilder<?, ?> unclaimedRewardRestEntity = UnclaimedRewardRestEntity.builder();

        unclaimedRewardRestEntity.address( unclaimedRewardRest.getAddress() );
        unclaimedRewardRestEntity.amount( unclaimedRewardRest.getAmount() );
        unclaimedRewardRestEntity.earnedEpoch( unclaimedRewardRest.getEarnedEpoch() );
        unclaimedRewardRestEntity.slot( unclaimedRewardRest.getSlot() );
        unclaimedRewardRestEntity.spendableEpoch( unclaimedRewardRest.getSpendableEpoch() );
        unclaimedRewardRestEntity.type( unclaimedRewardRest.getType() );

        return unclaimedRewardRestEntity.build();
    }

    @Override
    public EpochStake toEpochStake(EpochStakeEntity epochStakeEntity) {
        if ( epochStakeEntity == null ) {
            return null;
        }

        EpochStake.EpochStakeBuilder<?, ?> epochStake = EpochStake.builder();

        epochStake.activeEpoch( epochStakeEntity.getActiveEpoch() );
        epochStake.address( epochStakeEntity.getAddress() );
        epochStake.amount( epochStakeEntity.getAmount() );
        epochStake.delegationEpoch( epochStakeEntity.getDelegationEpoch() );
        epochStake.epoch( epochStakeEntity.getEpoch() );
        epochStake.poolId( epochStakeEntity.getPoolId() );

        return epochStake.build();
    }
}
