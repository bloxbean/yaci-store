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


    @Mapping(target = "costModels", ignore = true)
    @Mapping(target = "coinsPerUtxoSize", source = "adaPerUtxoByte")
    @Mapping(target = "a0", source = "poolPledgeInfluence")
    @Mapping(target = "rho", source = "expansionRate")
    @Mapping(target = "tau", source = "treasuryGrowthRate")
    @Mapping(target = "nOpt", source = "NOpt")
    @Mapping(target = "eMax", source = "maxEpoch")
    @Mapping(target = "pvtMotionNoConfidence", source = "poolVotingThresholds.pvtMotionNoConfidence")
    @Mapping(target = "pvtCommitteeNormal", source = "poolVotingThresholds.pvtCommitteeNormal")
    @Mapping(target = "pvtCommitteeNoConfidence", source = "poolVotingThresholds.pvtCommitteeNoConfidence")
    @Mapping(target = "pvtHardForkInitiation", source = "poolVotingThresholds.pvtHardForkInitiation")
    @Mapping(target = "dvtMotionNoConfidence", source = "drepVotingThresholds.dvtMotionNoConfidence")
    @Mapping(target = "dvtCommitteeNormal", source = "drepVotingThresholds.dvtCommitteeNormal")
    @Mapping(target = "dvtCommitteeNoConfidence", source = "drepVotingThresholds.dvtCommitteeNoConfidence")
    @Mapping(target = "dvtUpdateToConstitution", source = "drepVotingThresholds.dvtUpdateToConstitution")
    @Mapping(target = "dvtHardForkInitiation", source = "drepVotingThresholds.dvtHardForkInitiation")
    @Mapping(target = "dvtPPNetworkGroup", source = "drepVotingThresholds.dvtPPNetworkGroup")
    @Mapping(target = "dvtPPEconomicGroup", source = "drepVotingThresholds.dvtPPEconomicGroup")
    @Mapping(target = "dvtPPTechnicalGroup", source = "drepVotingThresholds.dvtPPTechnicalGroup")
    @Mapping(target = "dvtPPGovGroup", source = "drepVotingThresholds.dvtPPGovGroup")
    @Mapping(target = "dvtTreasuryWithdrawal", source = "drepVotingThresholds.dvtTreasuryWithdrawal")
    ProtocolParamsDto toProtocolParamsDto(ProtocolParams protocolParams);
}
