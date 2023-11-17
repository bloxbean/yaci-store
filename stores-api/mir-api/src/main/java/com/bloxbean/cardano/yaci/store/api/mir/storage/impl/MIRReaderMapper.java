package com.bloxbean.cardano.yaci.store.api.mir.storage.impl;

import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousRewardSummary;
import com.bloxbean.cardano.yaci.store.api.mir.projection.MIRSummary;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class MIRReaderMapper {
    public abstract MoveInstataneousRewardSummary toMoveInstataneousRewardSummary(MIRSummary mirSummary);
}
