package com.bloxbean.cardano.yaci.store.protocolparams.mapper;

import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import org.mapstruct.Mapping;

@org.mapstruct.Mapper(componentModel = "spring")
public abstract class DomainMapper {

    @Mapping(target = "costModels", ignore = true) //TODO
    public abstract ProtocolParams toProtocolParams(ProtocolParamUpdate protocolParamUpdate);
}
