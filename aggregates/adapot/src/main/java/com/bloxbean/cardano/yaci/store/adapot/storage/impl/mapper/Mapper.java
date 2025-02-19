package com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.adapot.domain.*;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.*;

@org.mapstruct.Mapper(componentModel = "spring")
public abstract class Mapper {
    public abstract InstantReward toInstantReward(InstantRewardEntity rewardEntity);
    public abstract InstantRewardEntity toInstantRewardEntity(InstantReward reward);

    public abstract RewardRest toRewardRest(RewardRestEntity rewardRestEntity);
    public abstract RewardRestEntity toRewardRestEntity(RewardRest rewardRest);

    public abstract Reward toReward(RewardEntity rewardEntity);
    public abstract RewardEntity toRewardEntity(Reward reward);

    public abstract UnclaimedRewardRest toUnclaimedRewardRest(UnclaimedRewardRestEntity unclaimedRewardRestEntity);
    public abstract UnclaimedRewardRestEntity toUnclaimedRewardRestEntity(UnclaimedRewardRest unclaimedRewardRest);

    public abstract EpochStake toEpochStake(EpochStakeEntity epochStakeEntity);

}
