package com.bloxbean.cardano.yaci.store.epoch.mapper;

import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(DomainMapperDecorator.class)
public interface DomainMapper {
    DomainMapper INSTANCE = Mappers.getMapper(DomainMapper.class);

    @Mapping(target = "costModels", ignore = true) //TODO
    @Mapping(target = "extraEntropy", ignore = true)
    ProtocolParams toProtocolParams(ProtocolParamUpdate protocolParamUpdate);

    @Mapping(target = "costModels", ignore = true)
    @Mapping(target = "coinsPerUtxoSize", source = "adaPerUtxoByte")
    com.bloxbean.cardano.client.api.model.ProtocolParams toCCLProtocolParams(ProtocolParams protocolParams);
}
