package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.Committee;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class CommitteeMapper {
    public abstract CommitteeEntity toCommitteeEntity(Committee committee);

    public abstract Committee toCommittee(CommitteeEntity committeeEntity);
}
