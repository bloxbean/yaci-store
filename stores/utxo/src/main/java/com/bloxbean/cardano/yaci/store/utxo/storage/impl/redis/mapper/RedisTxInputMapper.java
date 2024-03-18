package com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.mapper;

import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.model.RedisTxInputEntity;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(RedisTxInputMapperDecorator.class)
public interface RedisTxInputMapper {

    RedisTxInputMapper INSTANCE = Mappers.getMapper(RedisTxInputMapper.class);

    RedisTxInputEntity toRedisTxInputEntity(TxInput txInput);
}
