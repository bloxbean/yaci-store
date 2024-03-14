package com.bloxbean.cardano.yaci.store.account.storage.impl.mapper;


import com.bloxbean.cardano.yaci.store.account.domain.AddressTxAmount;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressTxAmountEntity;

public class AggrMapperDecorator implements AggrMapper {
    public static final int MAX_ADDR_SIZE = 500;
    private final AggrMapper delegate;

    public AggrMapperDecorator(AggrMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public AddressTxAmount toAddressTxAmount(AddressTxAmountEntity entity) {
        AddressTxAmount addrTxAmount = delegate.toAddressTxAmount(entity);

        if (entity.getAddrFull() != null && entity.getAddrFull().length() > 0)
            addrTxAmount.setAddress(entity.getAddrFull());

        return addrTxAmount;
    }

    @Override
    public AddressTxAmountEntity toAddressTxAmountEntity(AddressTxAmount addressTxAmount) {
        AddressTxAmountEntity entity = delegate.toAddressTxAmountEntity(addressTxAmount);

        if (addressTxAmount.getAddress() != null && addressTxAmount.getAddress().length() > MAX_ADDR_SIZE) {
            entity.setAddress(addressTxAmount.getAddress().substring(0, MAX_ADDR_SIZE));
            entity.setAddrFull(addressTxAmount.getAddress());
        }

        return entity;
    }

}
