package com.bloxbean.cardano.yaci.store.account.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressBalanceEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.StakeAddressBalanceEntity;

public class AccountMapperDecorator implements AccountMapper{
    public static final int MAX_ADDR_SIZE = 500;
    private final AccountMapper delegate;

    public AccountMapperDecorator(AccountMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public AddressBalance toAddressBalance(AddressBalanceEntity entity) {
        AddressBalance addressBalance = delegate.toAddressBalance(entity);

        if (entity.getAddrFull() != null && entity.getAddrFull().length() > 0)
            addressBalance.setAddress(entity.getAddrFull());

        return addressBalance;
    }

    @Override
    public AddressBalanceEntity toAddressBalanceEntity(AddressBalance addressBalance) {
        AddressBalanceEntity entity = delegate.toAddressBalanceEntity(addressBalance);

        if (addressBalance.getAddress() != null && addressBalance.getAddress().length() > MAX_ADDR_SIZE) {
            entity.setAddress(addressBalance.getAddress().substring(0, MAX_ADDR_SIZE));
            entity.setAddrFull(addressBalance.getAddress());
        }

        return entity;
    }

    @Override
    public StakeAddressBalance toStakeBalance(StakeAddressBalanceEntity entity) {
        return delegate.toStakeBalance(entity);
    }

    @Override
    public StakeAddressBalanceEntity toStakeBalanceEntity(StakeAddressBalance stakeBalance) {
        return delegate.toStakeBalanceEntity(stakeBalance);
    }
}
