package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.mapper;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.utxo.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.InvalidTransactionEntity;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(UtxoMapperDecorator.class)
public interface  UtxoMapper {
    UtxoMapper INSTANCE = Mappers.getMapper(UtxoMapper.class);

    AddressUtxoEntity toAddressUtxoEntity(AddressUtxo addressUtxo);
    AddressUtxo toAddressUtxo(AddressUtxoEntity entity);

    InvalidTransactionEntity toInvalidTransactionEntity(InvalidTransaction invalidTransaction);
    InvalidTransaction toInvalidTransaction(InvalidTransactionEntity invalidTransactionEntity);
}
