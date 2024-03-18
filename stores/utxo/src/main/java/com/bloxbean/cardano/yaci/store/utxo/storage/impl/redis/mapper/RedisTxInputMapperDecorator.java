package com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.mapper;

import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.model.RedisTxInputEntity;

public class RedisTxInputMapperDecorator implements RedisTxInputMapper {

    private final RedisTxInputMapper delegate;

    public RedisTxInputMapperDecorator(RedisTxInputMapper delegate) {
        this.delegate = delegate;
    }


    @Override
    public RedisTxInputEntity toRedisTxInputEntity(TxInput txInput) {
        RedisTxInputEntity entity = delegate.toRedisTxInputEntity(txInput);
        entity.setId(entity.getTxHash()+"#"+entity.getOutputIndex());
        return entity;
    }
}
