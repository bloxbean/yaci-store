package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalDRepDistr;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalDRepDistrEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class LocalDRepDistrMapper {
    public abstract LocalDRepDistr toLocalDRepDist(LocalDRepDistrEntity localDRepDistEntity);

    public abstract LocalDRepDistrEntity localDRepDistrEntity(LocalDRepDistr localDRepDist);
}
