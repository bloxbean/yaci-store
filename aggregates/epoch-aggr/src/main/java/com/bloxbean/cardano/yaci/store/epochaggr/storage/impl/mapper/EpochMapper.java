package com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.model.EpochEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", implementationName = "EpochMapperJpa")
public abstract class EpochMapper {

    public abstract Epoch toEpoch(EpochEntity blockEntity);
    public abstract EpochEntity toEpochEntity(Epoch blockEntity);

    public EpochEntity updateEntity(Epoch epoch, EpochEntity targetEntity) {
        targetEntity.setTotalOutput(epoch.getTotalOutput());
        targetEntity.setTransactionCount(epoch.getTransactionCount());
        targetEntity.setBlockCount(epoch.getBlockCount());
        targetEntity.setStartTime(epoch.getStartTime());
        targetEntity.setEndTime(epoch.getEndTime());
        targetEntity.setMaxSlot(epoch.getMaxSlot());

        return targetEntity;
    }
}
