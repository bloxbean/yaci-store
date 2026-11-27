package com.bloxbean.cardano.yaci.store.epoch.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.EpochParamEntity;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.ProtocolParamsProposalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class ProtocolParamsMapper {
    public abstract ProtocolParamsProposalEntity toEntity(ProtocolParamsProposal protocolParamsProposal);
    public abstract ProtocolParamsProposal toDomain(ProtocolParamsProposalEntity entity);

    @Mapping(target = "costModelHash", source = "epochParam.params.costModelsHash")
    @Mapping(target = "params.costModels", ignore = true) //set as null
    @Mapping(target = "params.nOpt", source = "params.NOpt")
    public abstract EpochParamEntity toEntity(EpochParam epochParam);
    //Copies params instead of sharing the managed entity's object; cost models are set from cost_model by storage
    @Mapping(target = "params.costModels", ignore = true)
    @Mapping(target = "params.nOpt", source = "params.NOpt")
    public abstract EpochParam toDomain(EpochParamEntity entity);
}
