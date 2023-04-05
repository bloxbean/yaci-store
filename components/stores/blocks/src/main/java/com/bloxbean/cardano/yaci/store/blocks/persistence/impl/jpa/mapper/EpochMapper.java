package com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.mapper;

import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.model.EpochEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class EpochMapper {
    public abstract Epoch toEpoch(EpochEntity blockEntity);
    public abstract EpochEntity toEpochEntity(Epoch blockEntity);
}
