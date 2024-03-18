package com.bloxbean.cardano.yaci.store.account.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.JpaAddressBalanceEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.JpaStakeAddressBalanceEntity;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(AccountMapperDecorator.class)
public interface AccountMapper {
    AccountMapper INSTANCE = Mappers.getMapper(AccountMapper.class);

    AddressBalance toAddressBalance(JpaAddressBalanceEntity entity);
    JpaAddressBalanceEntity toAddressBalanceEntity(AddressBalance addressBalance);

    StakeAddressBalance toStakeBalance(JpaStakeAddressBalanceEntity entity);
    JpaStakeAddressBalanceEntity toStakeBalanceEntity(StakeAddressBalance stakeBalance);

    AddressEntity toAddressEntity(AddressBalance addressBalance);
}
