package com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.model.JpaEpochEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", implementationName = "EpochMapperJpa")
public abstract class EpochMapper {

    public abstract Epoch toEpoch(JpaEpochEntity blockEntity);
    public abstract JpaEpochEntity toEpochEntity(Epoch blockEntity);

    public JpaEpochEntity updateEntity(Epoch epoch, JpaEpochEntity targetEntity) {
        targetEntity.setTotalOutput(epoch.getTotalOutput());
        targetEntity.setTransactionCount(epoch.getTransactionCount());
        targetEntity.setBlockCount(epoch.getBlockCount());
        targetEntity.setStartTime(epoch.getStartTime());
        targetEntity.setEndTime(epoch.getEndTime());
        targetEntity.setMaxSlot(epoch.getMaxSlot());

        return targetEntity;
    }
}
