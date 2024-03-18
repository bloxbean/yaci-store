package com.bloxbean.cardano.yaci.store.account.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressBalanceEntityJpa;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.StakeAddressBalanceEntityJpa;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(AccountMapperDecorator.class)
public interface AccountMapper {
    AccountMapper INSTANCE = Mappers.getMapper(AccountMapper.class);

    AddressBalance toAddressBalance(AddressBalanceEntityJpa entity);
    AddressBalanceEntityJpa toAddressBalanceEntity(AddressBalance addressBalance);

    StakeAddressBalance toStakeBalance(StakeAddressBalanceEntityJpa entity);
    StakeAddressBalanceEntityJpa toStakeBalanceEntity(StakeAddressBalance stakeBalance);

    AddressEntity toAddressEntity(AddressBalance addressBalance);
}
