package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRep;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DRepMapper {
    DRepEntity toDRepEntity(DRep dRep);
    DRep toDRep(DRepEntity dRepEntity);
}
