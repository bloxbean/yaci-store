package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.DRep;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DRepMapper {
    DRepEntity toDRepEntity(DRep dRep);
    DRep toDRep(DRepEntity dRepEntity);
}
