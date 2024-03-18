package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepRegistrationEntityJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class DRepRegistrationMapper {
    public abstract DRepRegistrationEntityJpa toDRepRegistrationEntity(DRepRegistration drepRegistration);

    public abstract DRepRegistration toDRepRegistration(DRepRegistrationEntityJpa drepRegistration);
}
