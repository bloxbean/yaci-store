package com.bloxbean.cardano.yaci.store.account.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.JpaAddressBalanceEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.JpaStakeAddressBalanceEntity;

public class AccountMapperDecorator implements AccountMapper{
    public static final int MAX_ADDR_SIZE = 500;
    private final AccountMapper delegate;

    public AccountMapperDecorator(AccountMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public AddressBalance toAddressBalance(JpaAddressBalanceEntity entity) {
        AddressBalance addressBalance = delegate.toAddressBalance(entity);

        if (entity.getAddrFull() != null && entity.getAddrFull().length() > 0)
            addressBalance.setAddress(entity.getAddrFull());

        return addressBalance;
    }

    @Override
    public JpaAddressBalanceEntity toAddressBalanceEntity(AddressBalance addressBalance) {
        JpaAddressBalanceEntity entity = delegate.toAddressBalanceEntity(addressBalance);

        if (addressBalance.getAddress() != null && addressBalance.getAddress().length() > MAX_ADDR_SIZE) {
            entity.setAddress(addressBalance.getAddress().substring(0, MAX_ADDR_SIZE));
            entity.setAddrFull(addressBalance.getAddress());
        }

        return entity;
    }

    @Override
    public StakeAddressBalance toStakeBalance(JpaStakeAddressBalanceEntity entity) {
        return delegate.toStakeBalance(entity);
    }

    @Override
    public JpaStakeAddressBalanceEntity toStakeBalanceEntity(StakeAddressBalance stakeBalance) {
        return delegate.toStakeBalanceEntity(stakeBalance);
    }

    @Override
    public AddressEntity toAddressEntity(AddressBalance addressBalance) {
        AddressEntity entity = delegate.toAddressEntity(addressBalance);

        if (addressBalance.getAddress() != null && addressBalance.getAddress().length() > MAX_ADDR_SIZE) {
            entity.setAddress(addressBalance.getAddress().substring(0, MAX_ADDR_SIZE));
            entity.setAddrFull(addressBalance.getAddress());
        }

        return entity;
    }
}
