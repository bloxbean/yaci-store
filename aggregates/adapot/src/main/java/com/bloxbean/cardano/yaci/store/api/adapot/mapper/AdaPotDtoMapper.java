package com.bloxbean.cardano.yaci.store.api.adapot.mapper;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.domain.UnclaimedRewardRest;
import com.bloxbean.cardano.yaci.store.api.adapot.dto.AdaPotDto;
import com.bloxbean.cardano.yaci.store.api.adapot.dto.RewardDto;
import com.bloxbean.cardano.yaci.store.api.adapot.dto.RewardRestDto;
import com.bloxbean.cardano.yaci.store.api.adapot.dto.UnclaimedRewardRestDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class AdaPotDtoMapper {
    public abstract AdaPotDto toAdaPotDto(AdaPot adaPot);
    public abstract RewardDto toRewardDto(Reward reward);
    public abstract RewardRestDto toRewardRestDto(RewardRest rewardRest);
    public abstract UnclaimedRewardRestDto unclaimedRewardRestDto(UnclaimedRewardRest unclaimedRewardRest);
}
