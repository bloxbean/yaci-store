package com.bloxbean.cardano.yaci.store.blockfrost.network.mapper;

import com.bloxbean.cardano.yaci.store.api.adapot.dto.NetworkInfoDto;
import com.bloxbean.cardano.yaci.store.blockfrost.network.dto.BFNetworkDto;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper that converts the internal {@link NetworkInfoDto} to the
 * Blockfrost-compatible {@link BFNetworkDto}.
 */
@Mapper(componentModel = "spring")
@DecoratedWith(BFNetworkMapperDecorator.class)
public interface BFNetworkMapper {

    BFNetworkMapper INSTANCE = Mappers.getMapper(BFNetworkMapper.class);

    @Mapping(target = "supply.max", source = "supply.max")
    @Mapping(target = "supply.circulating", source = "supply.circulating")
    @Mapping(target = "supply.treasury", source = "supply.treasury")
    @Mapping(target = "supply.reserves", source = "supply.reserves")
    @Mapping(target = "supply.total", ignore = true)
    @Mapping(target = "supply.locked", ignore = true)
    @Mapping(target = "stake.active", source = "stake.active")
    @Mapping(target = "stake.live", ignore = true)
    BFNetworkDto toBFNetworkDto(NetworkInfoDto networkInfoDto);
}
