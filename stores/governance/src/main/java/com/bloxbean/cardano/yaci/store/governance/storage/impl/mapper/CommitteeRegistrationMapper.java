package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeRegistrationEntityJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class CommitteeRegistrationMapper {
    public abstract CommitteeRegistrationEntityJpa toCommitteeRegistrationEntity(CommitteeRegistration committeeRegistration);

    public abstract CommitteeRegistration toCommitteeRegistration(CommitteeRegistrationEntityJpa committeeRegistrationEntity);
}
