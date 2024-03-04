package com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardAccount;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.EpochStakeEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardAccountEntity;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.RewardEntity;

@org.mapstruct.Mapper(componentModel = "spring")
public abstract class Mapper {
    public abstract Reward toReward(RewardEntity rewardEntity);
    public abstract RewardEntity toRewardEntity(Reward reward);

    public abstract RewardAccountEntity toRewardAccountEntity(RewardAccount rewardAccount);
    public abstract RewardAccount toRewardAccount(RewardAccountEntity rewardAccountEntity);

    public abstract EpochStake toEpochStake(EpochStakeEntity epochStakeEntity);

}