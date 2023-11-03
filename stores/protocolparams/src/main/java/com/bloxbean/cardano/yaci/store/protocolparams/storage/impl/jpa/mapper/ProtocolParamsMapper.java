package com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.mapper;

import com.bloxbean.cardano.yaci.store.protocolparams.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.protocolparams.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.model.EpochParamEntity;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.model.ProtocolParamsProposalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class ProtocolParamsMapper {
    public abstract ProtocolParamsProposalEntity toEntity(ProtocolParamsProposal protocolParamsProposal);
    public abstract ProtocolParamsProposal toDomain(ProtocolParamsProposalEntity entity);

    @Mapping(target = "costModelHash", source = "epochParam.params.costModelsHash")
    @Mapping(target = "params.costModels", ignore = true) //set as null
    public abstract EpochParamEntity toEntity(EpochParam epochParam);
    public abstract EpochParam toDomain(EpochParamEntity entity);
}
