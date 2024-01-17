package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepRegistrationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class DRepRegistrationMapper {
    public abstract DRepRegistrationEntity toDRepRegistrationEntity(DRepRegistration drepRegistration);

    public abstract DRepRegistration toDRepRegistration(DRepRegistrationEntity drepRegistration);
}
