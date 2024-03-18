package com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.model.EpochEntityJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", implementationName = "EpochMapperJpa")
public abstract class EpochMapper {

    public abstract Epoch toEpoch(EpochEntityJpa blockEntity);
    public abstract EpochEntityJpa toEpochEntity(Epoch blockEntity);

    public EpochEntityJpa updateEntity(Epoch epoch, EpochEntityJpa targetEntity) {
        targetEntity.setTotalOutput(epoch.getTotalOutput());
        targetEntity.setTransactionCount(epoch.getTransactionCount());
        targetEntity.setBlockCount(epoch.getBlockCount());
        targetEntity.setStartTime(epoch.getStartTime());
        targetEntity.setEndTime(epoch.getEndTime());
        targetEntity.setMaxSlot(epoch.getMaxSlot());

        return targetEntity;
    }
}
