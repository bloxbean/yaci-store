package com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.domain.InstantReward;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.EpochStakeEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.InstantRewardEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardEntity;

@org.mapstruct.Mapper(componentModel = "spring")
public abstract class Mapper {
    public abstract InstantReward toInstantReward(InstantRewardEntity rewardEntity);
    public abstract InstantRewardEntity toInstantRewardEntity(InstantReward reward);

    public abstract Reward toReward(RewardEntity rewardEntity);
    public abstract RewardEntity toRewardEntity(Reward reward);

    public abstract EpochStake toEpochStake(EpochStakeEntity epochStakeEntity);

}
