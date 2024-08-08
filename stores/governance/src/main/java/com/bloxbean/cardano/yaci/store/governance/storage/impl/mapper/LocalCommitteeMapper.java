package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalCommittee;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalCommitteeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class LocalCommitteeMapper {
    public abstract LocalCommittee toLocalCommittee(LocalCommitteeEntity localCommitteeEntity);
    public abstract LocalCommitteeEntity toLocalCommitteeEntity(LocalCommittee localCommittee);
}
