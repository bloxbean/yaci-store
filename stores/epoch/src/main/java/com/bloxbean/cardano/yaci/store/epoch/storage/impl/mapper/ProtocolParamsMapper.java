package com.bloxbean.cardano.yaci.store.epoch.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.EpochParamEntityJpa;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.ProtocolParamsProposalEntityJpa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class ProtocolParamsMapper {
    public abstract ProtocolParamsProposalEntityJpa toEntity(ProtocolParamsProposal protocolParamsProposal);
    public abstract ProtocolParamsProposal toDomain(ProtocolParamsProposalEntityJpa entity);

    @Mapping(target = "costModelHash", source = "epochParam.params.costModelsHash")
    @Mapping(target = "params.costModels", ignore = true) //set as null
    @Mapping(target = "params.nOpt", source = "params.NOpt")
    public abstract EpochParamEntityJpa toEntity(EpochParam epochParam);
    public abstract EpochParam toDomain(EpochParamEntityJpa entity);
}
