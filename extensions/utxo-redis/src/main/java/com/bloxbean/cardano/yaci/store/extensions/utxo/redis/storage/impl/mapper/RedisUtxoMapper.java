package com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.mapper;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.model.RedisAddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.model.RedisTxInputEntity;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.mapper.RedisUtxoMapperDecorator.class)
public interface RedisUtxoMapper {
    RedisUtxoMapper INSTANCE = Mappers.getMapper(RedisUtxoMapper.class);

    RedisAddressUtxoEntity toAddressUtxoEntity(AddressUtxo addressUtxo);

    AddressUtxo toAddressUtxo(RedisAddressUtxoEntity entity);

    TxInput toTxInput(RedisTxInputEntity redisTxInputEntity);
}
