package com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.mapper;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.model.RedisAddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.model.RedisAmt;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(RedisUtxoMapperDecorator.class)
public interface RedisUtxoMapper {
    RedisUtxoMapper INSTANCE = Mappers.getMapper(RedisUtxoMapper.class);

    RedisAddressUtxoEntity toAddressUtxoEntity(AddressUtxo addressUtxo);

    AddressUtxo toAddressUtxo(RedisAddressUtxoEntity entity);

    TxInput toTxInput(RedisAddressUtxoEntity entity);

    Amt toAmt(RedisAmt redisAmt);
}
