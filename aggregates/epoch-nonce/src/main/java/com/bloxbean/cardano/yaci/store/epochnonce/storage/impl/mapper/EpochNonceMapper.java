package com.bloxbean.cardano.yaci.store.epochnonce.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.epochnonce.domain.EpochNonce;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.impl.model.EpochNonceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class EpochNonceMapper {
    public abstract EpochNonce toEpochNonce(EpochNonceEntity entity);
    public abstract EpochNonceEntity toEpochNonceEntity(EpochNonce epochNonce);
}
