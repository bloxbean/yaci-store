package com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.mapper;

import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.model.RedisTxInputEntity;

public class RedisTxInputMapperDecorator implements com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.mapper.RedisTxInputMapper {

    private final com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.mapper.RedisTxInputMapper delegate;

    public RedisTxInputMapperDecorator(com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.mapper.RedisTxInputMapper delegate) {
        this.delegate = delegate;
    }


    @Override
    public RedisTxInputEntity toRedisTxInputEntity(TxInput txInput) {
        RedisTxInputEntity entity = delegate.toRedisTxInputEntity(txInput);
        entity.setId(entity.getTxHash()+"#"+entity.getOutputIndex());
        return entity;
    }
}
