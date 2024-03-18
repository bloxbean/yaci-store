package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.mapper;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.JpaAddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.JpaTxInputEntity;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(JpaUtxoMapperDecorator.class)
public interface JpaUtxoMapper {
    JpaUtxoMapper INSTANCE = Mappers.getMapper(JpaUtxoMapper.class);

    JpaAddressUtxoEntity toAddressUtxoEntity(AddressUtxo addressUtxo);
    AddressUtxo toAddressUtxo(JpaAddressUtxoEntity entity);

    TxInput toTxInput(JpaTxInputEntity jpaTxInputEntity);
}
