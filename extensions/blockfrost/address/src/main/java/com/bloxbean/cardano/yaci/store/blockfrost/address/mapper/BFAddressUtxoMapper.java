package com.bloxbean.cardano.yaci.store.blockfrost.address.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressUtxoDTO;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(BFAddressUtxoMapperDecorator.class)
public interface BFAddressUtxoMapper {

    BFAddressUtxoMapper INSTANCE = Mappers.getMapper(BFAddressUtxoMapper.class);

    @Mapping(target = "address", source = "ownerAddr")
    @Mapping(target = "txHash", source = "txHash")
    @Mapping(target = "outputIndex", source = "outputIndex")
    @Mapping(target = "amount", ignore = true)
    @Mapping(target = "block", source = "blockHash")
    @Mapping(target = "dataHash", source = "dataHash")
    @Mapping(target = "inlineDatum", source = "inlineDatum")
    @Mapping(target = "referenceScriptHash", source = "referenceScriptHash")
    BFAddressUtxoDTO toBFAddressUtxoDTO(AddressUtxo addressUtxo);
}

