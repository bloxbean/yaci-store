package com.bloxbean.cardano.yaci.store.mir.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousRewardSummary;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.model.MIREntityJpa;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.projection.MIRSummary;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class MIRMapper {
    public abstract MIREntityJpa toMIREntity(MoveInstataneousReward moveInstataneousReward);
    public abstract MoveInstataneousReward toMoveInstataneousReward(MIREntityJpa mirEntity);

    public abstract MoveInstataneousRewardSummary toMoveInstataneousRewardSummary(MIRSummary mirSummary);
}
