package com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.mapper;

import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.model.TxnEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class TxnMapper {
    public abstract TxnEntity toTxnEntity(Txn txn);
    public abstract Txn toTxn(TxnEntity entity);
}
