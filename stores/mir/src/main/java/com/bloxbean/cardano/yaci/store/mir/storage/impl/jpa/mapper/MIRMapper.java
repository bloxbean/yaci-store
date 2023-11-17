package com.bloxbean.cardano.yaci.store.mir.storage.impl.jpa.mapper;

import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.jpa.model.MIREntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class MIRMapper {
    public abstract MIREntity toMIREntity(MoveInstataneousReward moveInstataneousReward);
    public abstract MoveInstataneousReward toMoveInstataneousReward(MIREntity mirEntity);
}
