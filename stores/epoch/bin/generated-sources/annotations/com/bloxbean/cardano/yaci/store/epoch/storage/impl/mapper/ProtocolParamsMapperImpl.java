package com.bloxbean.cardano.yaci.store.epoch.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.domain.ProtocolParamsProposal;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.EpochParamEntity;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.ProtocolParamsProposalEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:20+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class ProtocolParamsMapperImpl extends ProtocolParamsMapper {

    @Override
    public ProtocolParamsProposalEntity toEntity(ProtocolParamsProposal protocolParamsProposal) {
        if ( protocolParamsProposal == null ) {
            return null;
        }

        ProtocolParamsProposalEntity.ProtocolParamsProposalEntityBuilder<?, ?> protocolParamsProposalEntity = ProtocolParamsProposalEntity.builder();

        protocolParamsProposalEntity.blockNumber( protocolParamsProposal.getBlockNumber() );
        protocolParamsProposalEntity.blockTime( protocolParamsProposal.getBlockTime() );
        protocolParamsProposalEntity.epoch( protocolParamsProposal.getEpoch() );
        protocolParamsProposalEntity.era( protocolParamsProposal.getEra() );
        protocolParamsProposalEntity.keyHash( protocolParamsProposal.getKeyHash() );
        protocolParamsProposalEntity.params( protocolParamsProposal.getParams() );
        protocolParamsProposalEntity.slot( protocolParamsProposal.getSlot() );
        protocolParamsProposalEntity.targetEpoch( protocolParamsProposal.getTargetEpoch() );
        protocolParamsProposalEntity.txHash( protocolParamsProposal.getTxHash() );

        return protocolParamsProposalEntity.build();
    }

    @Override
    public ProtocolParamsProposal toDomain(ProtocolParamsProposalEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ProtocolParamsProposal.ProtocolParamsProposalBuilder<?, ?> protocolParamsProposal = ProtocolParamsProposal.builder();

        protocolParamsProposal.blockNumber( entity.getBlockNumber() );
        protocolParamsProposal.blockTime( entity.getBlockTime() );
        protocolParamsProposal.epoch( entity.getEpoch() );
        protocolParamsProposal.era( entity.getEra() );
        protocolParamsProposal.keyHash( entity.getKeyHash() );
        protocolParamsProposal.params( entity.getParams() );
        protocolParamsProposal.slot( entity.getSlot() );
        protocolParamsProposal.targetEpoch( entity.getTargetEpoch() );
        protocolParamsProposal.txHash( entity.getTxHash() );

        return protocolParamsProposal.build();
    }

    @Override
    public EpochParamEntity toEntity(EpochParam epochParam) {
        if ( epochParam == null ) {
            return null;
        }

        EpochParamEntity.EpochParamEntityBuilder<?, ?> epochParamEntity = EpochParamEntity.builder();

        epochParamEntity.params( protocolParamsToProtocolParams( epochParam.getParams() ) );
        epochParamEntity.costModelHash( epochParamParamsCostModelsHash( epochParam ) );
        epochParamEntity.blockNumber( epochParam.getBlockNumber() );
        epochParamEntity.blockTime( epochParam.getBlockTime() );
        epochParamEntity.epoch( epochParam.getEpoch() );
        epochParamEntity.slot( epochParam.getSlot() );

        return epochParamEntity.build();
    }

    @Override
    public EpochParam toDomain(EpochParamEntity entity) {
        if ( entity == null ) {
            return null;
        }

        EpochParam.EpochParamBuilder<?, ?> epochParam = EpochParam.builder();

        epochParam.blockNumber( entity.getBlockNumber() );
        epochParam.blockTime( entity.getBlockTime() );
        epochParam.epoch( entity.getEpoch() );
        epochParam.params( entity.getParams() );
        epochParam.slot( entity.getSlot() );

        return epochParam.build();
    }

    protected ProtocolParams protocolParamsToProtocolParams(ProtocolParams protocolParams) {
        if ( protocolParams == null ) {
            return null;
        }

        ProtocolParams.ProtocolParamsBuilder protocolParams1 = ProtocolParams.builder();

        protocolParams1.nOpt( protocolParams.getNOpt() );
        protocolParams1.adaPerUtxoByte( protocolParams.getAdaPerUtxoByte() );
        protocolParams1.collateralPercent( protocolParams.getCollateralPercent() );
        protocolParams1.committeeMaxTermLength( protocolParams.getCommitteeMaxTermLength() );
        protocolParams1.committeeMinSize( protocolParams.getCommitteeMinSize() );
        protocolParams1.costModelsHash( protocolParams.getCostModelsHash() );
        protocolParams1.decentralisationParam( protocolParams.getDecentralisationParam() );
        protocolParams1.drepActivity( protocolParams.getDrepActivity() );
        protocolParams1.drepDeposit( protocolParams.getDrepDeposit() );
        protocolParams1.drepVotingThresholds( protocolParams.getDrepVotingThresholds() );
        protocolParams1.expansionRate( protocolParams.getExpansionRate() );
        protocolParams1.extraEntropy( protocolParams.getExtraEntropy() );
        protocolParams1.govActionDeposit( protocolParams.getGovActionDeposit() );
        protocolParams1.govActionLifetime( protocolParams.getGovActionLifetime() );
        protocolParams1.keyDeposit( protocolParams.getKeyDeposit() );
        protocolParams1.maxBlockExMem( protocolParams.getMaxBlockExMem() );
        protocolParams1.maxBlockExSteps( protocolParams.getMaxBlockExSteps() );
        protocolParams1.maxBlockHeaderSize( protocolParams.getMaxBlockHeaderSize() );
        protocolParams1.maxBlockSize( protocolParams.getMaxBlockSize() );
        protocolParams1.maxCollateralInputs( protocolParams.getMaxCollateralInputs() );
        protocolParams1.maxEpoch( protocolParams.getMaxEpoch() );
        protocolParams1.maxTxExMem( protocolParams.getMaxTxExMem() );
        protocolParams1.maxTxExSteps( protocolParams.getMaxTxExSteps() );
        protocolParams1.maxTxSize( protocolParams.getMaxTxSize() );
        protocolParams1.maxValSize( protocolParams.getMaxValSize() );
        protocolParams1.minFeeA( protocolParams.getMinFeeA() );
        protocolParams1.minFeeB( protocolParams.getMinFeeB() );
        protocolParams1.minFeeRefScriptCostPerByte( protocolParams.getMinFeeRefScriptCostPerByte() );
        protocolParams1.minPoolCost( protocolParams.getMinPoolCost() );
        protocolParams1.minUtxo( protocolParams.getMinUtxo() );
        protocolParams1.poolDeposit( protocolParams.getPoolDeposit() );
        protocolParams1.poolPledgeInfluence( protocolParams.getPoolPledgeInfluence() );
        protocolParams1.poolVotingThresholds( protocolParams.getPoolVotingThresholds() );
        protocolParams1.priceMem( protocolParams.getPriceMem() );
        protocolParams1.priceStep( protocolParams.getPriceStep() );
        protocolParams1.protocolMajorVer( protocolParams.getProtocolMajorVer() );
        protocolParams1.protocolMinorVer( protocolParams.getProtocolMinorVer() );
        protocolParams1.treasuryGrowthRate( protocolParams.getTreasuryGrowthRate() );

        return protocolParams1.build();
    }

    private String epochParamParamsCostModelsHash(EpochParam epochParam) {
        if ( epochParam == null ) {
            return null;
        }
        ProtocolParams params = epochParam.getParams();
        if ( params == null ) {
            return null;
        }
        String costModelsHash = params.getCostModelsHash();
        if ( costModelsHash == null ) {
            return null;
        }
        return costModelsHash;
    }
}
