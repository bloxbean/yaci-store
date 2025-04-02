package com.bloxbean.cardano.yaci.store.epoch.mapper;

import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
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
    @Mapping(target = "nOpt", source = "NOpt")
    ProtocolParams toProtocolParams(ProtocolParamUpdate protocolParamUpdate);


    @Mapping(target = "nOpt", source = "NOpt")
    @Mapping(target = "eMax", source = "maxEpoch")
    @Mapping(target = "coinsPerUtxoSize", source = "adaPerUtxoByte")
    @Mapping(target = "costModels", ignore = true)
    @Mapping(target = "a0", ignore = true)
    @Mapping(target = "rho", ignore = true)
    @Mapping(target = "tau", ignore = true)
    @Mapping(target = "decentralisationParam", ignore = true)
    @Mapping(target = "priceMem", ignore = true)
    @Mapping(target = "priceStep", ignore = true)
    @Mapping(target = "pvtMotionNoConfidence", ignore = true)
    @Mapping(target = "pvtCommitteeNormal", ignore = true)
    @Mapping(target = "pvtCommitteeNoConfidence", ignore = true)
    @Mapping(target = "pvtHardForkInitiation", ignore = true)
    @Mapping(target = "pvtPPSecurityGroup", ignore = true)
    @Mapping(target = "dvtMotionNoConfidence", ignore = true)
    @Mapping(target = "dvtCommitteeNormal", ignore = true)
    @Mapping(target = "dvtCommitteeNoConfidence", ignore = true)
    @Mapping(target = "dvtUpdateToConstitution", ignore = true)
    @Mapping(target = "dvtHardForkInitiation", ignore = true)
    @Mapping(target = "dvtPPNetworkGroup", ignore = true)
    @Mapping(target = "dvtPPEconomicGroup", ignore = true)
    @Mapping(target = "dvtPPTechnicalGroup", ignore = true)
    @Mapping(target = "dvtPPGovGroup", ignore = true)
    @Mapping(target = "dvtTreasuryWithdrawal", ignore = true)
    @Mapping(target = "minFeeRefScriptCostPerByte", ignore = true)
    ProtocolParamsDto toProtocolParamsDto(ProtocolParams protocolParams);
}
