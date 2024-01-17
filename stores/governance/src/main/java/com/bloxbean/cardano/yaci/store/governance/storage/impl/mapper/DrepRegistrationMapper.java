package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.DrepRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DrepRegistrationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class DrepRegistrationMapper {
    public abstract DrepRegistrationEntity toDrepRegistrationEntity(DrepRegistration drepRegistration);

    public abstract DrepRegistration toDrepRegistration(DrepRegistrationEntity drepRegistration);
}
