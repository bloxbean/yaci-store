package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepExpiry;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepExpiryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DRepExpiryMapper {
    DRepExpiry toDRepExpiry(DRepExpiryEntity dRepExpiryEntity);
    DRepExpiryEntity toDRepExpiryEntity(DRepExpiry dRepExpiry);
}
