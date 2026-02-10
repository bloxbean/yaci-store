package com.bloxbean.cardano.yaci.store.blockfrost.epoch.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFProtocolParamsDto;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(BFProtocolParamMapperDecorator.class)
public interface BFProtocolParamMapper {

    BFProtocolParamMapper INSTANCE = Mappers.getMapper(BFProtocolParamMapper.class);


    BFProtocolParamsDto toBFProtocolParamsDto(ProtocolParamsDto protocolParamsDto);
}
