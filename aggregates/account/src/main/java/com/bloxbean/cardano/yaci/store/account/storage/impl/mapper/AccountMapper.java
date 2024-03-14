package com.bloxbean.cardano.yaci.store.account.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressBalanceEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.StakeAddressBalanceEntity;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(AccountMapperDecorator.class)
public interface AccountMapper {
    AccountMapper INSTANCE = Mappers.getMapper(AccountMapper.class);

    AddressBalance toAddressBalance(AddressBalanceEntity entity);
    AddressBalanceEntity toAddressBalanceEntity(AddressBalance addressBalance);

    StakeAddressBalance toStakeBalance(StakeAddressBalanceEntity entity);
    StakeAddressBalanceEntity toStakeBalanceEntity(StakeAddressBalance stakeBalance);

    AddressEntity toAddressEntity(AddressBalance addressBalance);
}
