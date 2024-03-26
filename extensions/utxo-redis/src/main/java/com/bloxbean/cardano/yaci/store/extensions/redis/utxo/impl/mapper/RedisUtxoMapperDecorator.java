package com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.mapper;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.model.RedisAddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.model.RedisAmt;
import com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.model.RedisTxInputEntity;

public class RedisUtxoMapperDecorator implements RedisUtxoMapper {

    public static final int MAX_ADDR_SIZE = 500;
    private final RedisUtxoMapper delegate;

    public RedisUtxoMapperDecorator(RedisUtxoMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public RedisAddressUtxoEntity toAddressUtxoEntity(AddressUtxo addressUtxo) {
        RedisAddressUtxoEntity entity = delegate.toAddressUtxoEntity(addressUtxo);

        if (addressUtxo.getOwnerAddr() != null && addressUtxo.getOwnerAddr().length() > MAX_ADDR_SIZE) {
            entity.setOwnerAddr(addressUtxo.getOwnerAddr().substring(0, MAX_ADDR_SIZE));
            entity.setOwnerAddrFull(addressUtxo.getOwnerAddr());
        }
        entity.setId(addressUtxo.getTxHash()+"#"+addressUtxo.getOutputIndex());

        return entity;
    }

    @Override
    public AddressUtxo toAddressUtxo(RedisAddressUtxoEntity entity) {
        AddressUtxo addressUtxo = delegate.toAddressUtxo(entity);

        if (entity.getOwnerAddrFull() != null && !entity.getOwnerAddrFull().isEmpty())
            addressUtxo.setOwnerAddr(entity.getOwnerAddrFull());

        return addressUtxo;
    }

    @Override
    public TxInput toTxInput(RedisTxInputEntity redisTxInputEntity) {
        return delegate.toTxInput(redisTxInputEntity);
    }

    @Override
    public Amt toAmt(RedisAmt redisAmt) {
        return delegate.toAmt(redisAmt);
    }

}
