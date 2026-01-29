package com.bloxbean.cardano.yaci.store.blockfrost.address.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressTransactionDTO;
import com.bloxbean.cardano.yaci.store.blockfrost.address.dto.BFAddressUtxoDTO;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressTransaction;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BFAddressTransactionMapper {

    BFAddressTransactionMapper INSTANCE = Mappers.getMapper(BFAddressTransactionMapper.class);

    @Mapping(target = "txHash", source = "txHash")
    @Mapping(target = "txIndex", ignore = true)
    @Mapping(target = "blockHeight", source = "blockHeight")
    @Mapping(target = "blockTime", source = "blockTime")
    BFAddressTransactionDTO toBFAddressTransactionDTO(AddressTransaction addressTransaction);
}

