package com.bloxbean.cardano.yaci.store.account.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.account.domain.AddressTxAmount;
import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AddressTxAmountEntity;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "default")
@DecoratedWith(AggrMapperDecorator.class)
public interface AggrMapper {
    AggrMapper INSTANCE = Mappers.getMapper(AggrMapper.class);

    AddressTxAmount toAddressTxAmount(AddressTxAmountEntity entity);
    AddressTxAmountEntity toAddressTxAmountEntity(AddressTxAmount addressTxAmount);
}

