package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeDeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeDeRegistrationEntityJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class CommitteeDeRegistrationMapper {
    public abstract CommitteeDeRegistrationEntityJpa toCommitteeDeRegistrationEntity(CommitteeDeRegistration committeeDeRegistration);

    public abstract CommitteeDeRegistration toCommitteeDeRegistration(CommitteeDeRegistrationEntityJpa committeeDeRegistrationEntity);
}
