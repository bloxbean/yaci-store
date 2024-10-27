package com.bloxbean.cardano.yaci.store.adapot.reward.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.reward.domain.RewardCalcJob;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class RewardCalcJobMapper    {
    public abstract RewardCalcJob toDomain(RewardCalcJobEntity rewardCalcJobEntity);
    public abstract RewardCalcJobEntity toEntity(RewardCalcJob rewardCalcJob);
}
