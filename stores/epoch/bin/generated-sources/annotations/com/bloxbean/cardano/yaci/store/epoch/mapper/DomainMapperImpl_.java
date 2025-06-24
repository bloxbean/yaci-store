package com.bloxbean.cardano.yaci.store.epoch.mapper;

import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import java.math.BigDecimal;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:20+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
public class DomainMapperImpl_ implements DomainMapper {

    @Override
    public ProtocolParams toProtocolParams(ProtocolParamUpdate protocolParamUpdate) {
        if ( protocolParamUpdate == null ) {
            return null;
        }

        ProtocolParams.ProtocolParamsBuilder protocolParams = ProtocolParams.builder();

        protocolParams.nOpt( protocolParamUpdate.getNOpt() );
        protocolParams.adaPerUtxoByte( protocolParamUpdate.getAdaPerUtxoByte() );
        protocolParams.collateralPercent( protocolParamUpdate.getCollateralPercent() );
        protocolParams.committeeMaxTermLength( protocolParamUpdate.getCommitteeMaxTermLength() );
        protocolParams.committeeMinSize( protocolParamUpdate.getCommitteeMinSize() );
        protocolParams.costModelsHash( protocolParamUpdate.getCostModelsHash() );
        protocolParams.decentralisationParam( protocolParamUpdate.getDecentralisationParam() );
        protocolParams.drepActivity( protocolParamUpdate.getDrepActivity() );
        protocolParams.drepDeposit( protocolParamUpdate.getDrepDeposit() );
        protocolParams.drepVotingThresholds( drepVoteThresholdsToDrepVoteThresholds( protocolParamUpdate.getDrepVotingThresholds() ) );
        protocolParams.expansionRate( protocolParamUpdate.getExpansionRate() );
        protocolParams.govActionDeposit( protocolParamUpdate.getGovActionDeposit() );
        protocolParams.govActionLifetime( protocolParamUpdate.getGovActionLifetime() );
        protocolParams.keyDeposit( protocolParamUpdate.getKeyDeposit() );
        protocolParams.maxBlockExMem( protocolParamUpdate.getMaxBlockExMem() );
        protocolParams.maxBlockExSteps( protocolParamUpdate.getMaxBlockExSteps() );
        protocolParams.maxBlockHeaderSize( protocolParamUpdate.getMaxBlockHeaderSize() );
        protocolParams.maxBlockSize( protocolParamUpdate.getMaxBlockSize() );
        protocolParams.maxCollateralInputs( protocolParamUpdate.getMaxCollateralInputs() );
        protocolParams.maxEpoch( protocolParamUpdate.getMaxEpoch() );
        protocolParams.maxTxExMem( protocolParamUpdate.getMaxTxExMem() );
        protocolParams.maxTxExSteps( protocolParamUpdate.getMaxTxExSteps() );
        protocolParams.maxTxSize( protocolParamUpdate.getMaxTxSize() );
        protocolParams.maxValSize( protocolParamUpdate.getMaxValSize() );
        protocolParams.minFeeA( protocolParamUpdate.getMinFeeA() );
        protocolParams.minFeeB( protocolParamUpdate.getMinFeeB() );
        protocolParams.minFeeRefScriptCostPerByte( protocolParamUpdate.getMinFeeRefScriptCostPerByte() );
        protocolParams.minPoolCost( protocolParamUpdate.getMinPoolCost() );
        protocolParams.minUtxo( protocolParamUpdate.getMinUtxo() );
        protocolParams.poolDeposit( protocolParamUpdate.getPoolDeposit() );
        protocolParams.poolPledgeInfluence( protocolParamUpdate.getPoolPledgeInfluence() );
        protocolParams.poolVotingThresholds( poolVotingThresholdsToPoolVotingThresholds( protocolParamUpdate.getPoolVotingThresholds() ) );
        protocolParams.priceMem( protocolParamUpdate.getPriceMem() );
        protocolParams.priceStep( protocolParamUpdate.getPriceStep() );
        protocolParams.protocolMajorVer( protocolParamUpdate.getProtocolMajorVer() );
        protocolParams.protocolMinorVer( protocolParamUpdate.getProtocolMinorVer() );
        protocolParams.treasuryGrowthRate( protocolParamUpdate.getTreasuryGrowthRate() );

        return protocolParams.build();
    }

    @Override
    public ProtocolParamsDto toProtocolParamsDto(ProtocolParams protocolParams) {
        if ( protocolParams == null ) {
            return null;
        }

        ProtocolParamsDto.ProtocolParamsDtoBuilder protocolParamsDto = ProtocolParamsDto.builder();

        protocolParamsDto.nOpt( protocolParams.getNOpt() );
        protocolParamsDto.eMax( protocolParams.getMaxEpoch() );
        if ( protocolParams.getAdaPerUtxoByte() != null ) {
            protocolParamsDto.coinsPerUtxoSize( protocolParams.getAdaPerUtxoByte().toString() );
        }
        if ( protocolParams.getCollateralPercent() != null ) {
            protocolParamsDto.collateralPercent( BigDecimal.valueOf( protocolParams.getCollateralPercent() ) );
        }
        protocolParamsDto.committeeMaxTermLength( protocolParams.getCommitteeMaxTermLength() );
        protocolParamsDto.committeeMinSize( protocolParams.getCommitteeMinSize() );
        protocolParamsDto.drepActivity( protocolParams.getDrepActivity() );
        protocolParamsDto.drepDeposit( protocolParams.getDrepDeposit() );
        protocolParamsDto.extraEntropy( protocolParams.getExtraEntropy() );
        protocolParamsDto.govActionDeposit( protocolParams.getGovActionDeposit() );
        protocolParamsDto.govActionLifetime( protocolParams.getGovActionLifetime() );
        if ( protocolParams.getKeyDeposit() != null ) {
            protocolParamsDto.keyDeposit( protocolParams.getKeyDeposit().toString() );
        }
        if ( protocolParams.getMaxBlockExMem() != null ) {
            protocolParamsDto.maxBlockExMem( protocolParams.getMaxBlockExMem().toString() );
        }
        if ( protocolParams.getMaxBlockExSteps() != null ) {
            protocolParamsDto.maxBlockExSteps( protocolParams.getMaxBlockExSteps().toString() );
        }
        protocolParamsDto.maxBlockHeaderSize( protocolParams.getMaxBlockHeaderSize() );
        protocolParamsDto.maxBlockSize( protocolParams.getMaxBlockSize() );
        protocolParamsDto.maxCollateralInputs( protocolParams.getMaxCollateralInputs() );
        if ( protocolParams.getMaxTxExMem() != null ) {
            protocolParamsDto.maxTxExMem( protocolParams.getMaxTxExMem().toString() );
        }
        if ( protocolParams.getMaxTxExSteps() != null ) {
            protocolParamsDto.maxTxExSteps( protocolParams.getMaxTxExSteps().toString() );
        }
        protocolParamsDto.maxTxSize( protocolParams.getMaxTxSize() );
        if ( protocolParams.getMaxValSize() != null ) {
            protocolParamsDto.maxValSize( String.valueOf( protocolParams.getMaxValSize() ) );
        }
        protocolParamsDto.minFeeA( protocolParams.getMinFeeA() );
        protocolParamsDto.minFeeB( protocolParams.getMinFeeB() );
        if ( protocolParams.getMinPoolCost() != null ) {
            protocolParamsDto.minPoolCost( protocolParams.getMinPoolCost().toString() );
        }
        if ( protocolParams.getMinUtxo() != null ) {
            protocolParamsDto.minUtxo( protocolParams.getMinUtxo().toString() );
        }
        if ( protocolParams.getPoolDeposit() != null ) {
            protocolParamsDto.poolDeposit( protocolParams.getPoolDeposit().toString() );
        }
        protocolParamsDto.protocolMajorVer( protocolParams.getProtocolMajorVer() );
        protocolParamsDto.protocolMinorVer( protocolParams.getProtocolMinorVer() );

        return protocolParamsDto.build();
    }

    protected DrepVoteThresholds drepVoteThresholdsToDrepVoteThresholds(com.bloxbean.cardano.yaci.core.model.DrepVoteThresholds drepVoteThresholds) {
        if ( drepVoteThresholds == null ) {
            return null;
        }

        DrepVoteThresholds.DrepVoteThresholdsBuilder drepVoteThresholds1 = DrepVoteThresholds.builder();

        drepVoteThresholds1.dvtCommitteeNoConfidence( drepVoteThresholds.getDvtCommitteeNoConfidence() );
        drepVoteThresholds1.dvtCommitteeNormal( drepVoteThresholds.getDvtCommitteeNormal() );
        drepVoteThresholds1.dvtHardForkInitiation( drepVoteThresholds.getDvtHardForkInitiation() );
        drepVoteThresholds1.dvtMotionNoConfidence( drepVoteThresholds.getDvtMotionNoConfidence() );
        drepVoteThresholds1.dvtPPEconomicGroup( drepVoteThresholds.getDvtPPEconomicGroup() );
        drepVoteThresholds1.dvtPPGovGroup( drepVoteThresholds.getDvtPPGovGroup() );
        drepVoteThresholds1.dvtPPNetworkGroup( drepVoteThresholds.getDvtPPNetworkGroup() );
        drepVoteThresholds1.dvtPPTechnicalGroup( drepVoteThresholds.getDvtPPTechnicalGroup() );
        drepVoteThresholds1.dvtTreasuryWithdrawal( drepVoteThresholds.getDvtTreasuryWithdrawal() );
        drepVoteThresholds1.dvtUpdateToConstitution( drepVoteThresholds.getDvtUpdateToConstitution() );

        return drepVoteThresholds1.build();
    }

    protected PoolVotingThresholds poolVotingThresholdsToPoolVotingThresholds(com.bloxbean.cardano.yaci.core.model.PoolVotingThresholds poolVotingThresholds) {
        if ( poolVotingThresholds == null ) {
            return null;
        }

        PoolVotingThresholds.PoolVotingThresholdsBuilder poolVotingThresholds1 = PoolVotingThresholds.builder();

        poolVotingThresholds1.pvtCommitteeNoConfidence( poolVotingThresholds.getPvtCommitteeNoConfidence() );
        poolVotingThresholds1.pvtCommitteeNormal( poolVotingThresholds.getPvtCommitteeNormal() );
        poolVotingThresholds1.pvtHardForkInitiation( poolVotingThresholds.getPvtHardForkInitiation() );
        poolVotingThresholds1.pvtMotionNoConfidence( poolVotingThresholds.getPvtMotionNoConfidence() );
        poolVotingThresholds1.pvtPPSecurityGroup( poolVotingThresholds.getPvtPPSecurityGroup() );

        return poolVotingThresholds1.build();
    }
}
