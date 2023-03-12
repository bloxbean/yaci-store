package com.bloxbean.cardano.yaci.store.utxo.mapper;

import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.utxo.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.utxo.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.model.InvalidTransactionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class UtxoMapper {
    public abstract AddressUtxoEntity toAddressUtxoEntity(AddressUtxo addressUtxo);
    public abstract AddressUtxo toAddressUtxo(AddressUtxoEntity entity);

    public abstract InvalidTransactionEntity toInvalidTransactionEntity(InvalidTransaction invalidTransaction);
    public abstract InvalidTransaction toInvalidTransaction(InvalidTransactionEntity invalidTransactionEntity);
}
