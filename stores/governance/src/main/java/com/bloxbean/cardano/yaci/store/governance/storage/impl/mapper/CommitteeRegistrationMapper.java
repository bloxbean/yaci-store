package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeRegistrationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class CommitteeRegistrationMapper {
    public abstract CommitteeRegistrationEntity toCommitteeRegistrationEntity(CommitteeRegistration committeeRegistration);

    public abstract CommitteeRegistration toCommitteeRegistration(CommitteeRegistrationEntity committeeRegistrationEntity);
}
