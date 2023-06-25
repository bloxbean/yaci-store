package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.mapper;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.utxo.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.InvalidTransactionEntity;

public class UtxoMapperDecorator implements UtxoMapper {
    public static final int MAX_ADDR_SIZE = 500;
    private final UtxoMapper delegate;

    public UtxoMapperDecorator(UtxoMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public AddressUtxoEntity toAddressUtxoEntity(AddressUtxo addressUtxo) {
        AddressUtxoEntity entity = delegate.toAddressUtxoEntity(addressUtxo);

        if (addressUtxo.getOwnerAddr() != null && addressUtxo.getOwnerAddr().length() > MAX_ADDR_SIZE) {
            entity.setOwnerAddr(addressUtxo.getOwnerAddr().substring(0, MAX_ADDR_SIZE));
            entity.setOwnerAddrFull(addressUtxo.getOwnerAddr());
        }

        return entity;
    }

    @Override
    public AddressUtxo toAddressUtxo(AddressUtxoEntity entity) {
        AddressUtxo addressUtxo = delegate.toAddressUtxo(entity);

        if (entity.getOwnerAddrFull() != null && entity.getOwnerAddrFull().length() > 0)
            addressUtxo.setOwnerAddr(entity.getOwnerAddrFull());

        return addressUtxo;
    }

    @Override
    public InvalidTransactionEntity toInvalidTransactionEntity(InvalidTransaction invalidTransaction) {
        return delegate.toInvalidTransactionEntity(invalidTransaction);
    }

    @Override
    public InvalidTransaction toInvalidTransaction(InvalidTransactionEntity invalidTransactionEntity) {
        return delegate.toInvalidTransaction(invalidTransactionEntity);
    }
}
