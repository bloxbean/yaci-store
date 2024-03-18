package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.mapper;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.JpaAddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.JpaTxInputEntity;

public class JpaUtxoMapperDecorator implements JpaUtxoMapper {
    public static final int MAX_ADDR_SIZE = 500;
    private final JpaUtxoMapper delegate;

    public JpaUtxoMapperDecorator(JpaUtxoMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public JpaAddressUtxoEntity toAddressUtxoEntity(AddressUtxo addressUtxo) {
        JpaAddressUtxoEntity entity = delegate.toAddressUtxoEntity(addressUtxo);

        if (addressUtxo.getOwnerAddr() != null && addressUtxo.getOwnerAddr().length() > MAX_ADDR_SIZE) {
            entity.setOwnerAddr(addressUtxo.getOwnerAddr().substring(0, MAX_ADDR_SIZE));
            entity.setOwnerAddrFull(addressUtxo.getOwnerAddr());
        }

        return entity;
    }

    @Override
    public AddressUtxo toAddressUtxo(JpaAddressUtxoEntity entity) {
        AddressUtxo addressUtxo = delegate.toAddressUtxo(entity);

        if (entity.getOwnerAddrFull() != null && entity.getOwnerAddrFull().length() > 0)
            addressUtxo.setOwnerAddr(entity.getOwnerAddrFull());

        return addressUtxo;
    }

    @Override
    public TxInput toTxInput(JpaTxInputEntity jpaTxInputEntity) {
        return delegate.toTxInput(jpaTxInputEntity);
    }

}
