package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalHardForkInitiation;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalHardForkInitiationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class LocalHardForkInitiationMapper {
    public abstract LocalHardForkInitiationEntity toLocalHardForkInitiationEntity(LocalHardForkInitiation localHardForkInitiation);

    public abstract LocalHardForkInitiation toLocalHardForkInitiation(LocalHardForkInitiationEntity localHardForkInitiationEntity);
}
