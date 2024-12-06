package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepDist;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepDistEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DRepDistMapper {
    DRepDist toDRepDist(DRepDistEntity dRepDistEntity);
    DRepDistEntity toDRepDistEntity(DRepDist dRepDist);
}
