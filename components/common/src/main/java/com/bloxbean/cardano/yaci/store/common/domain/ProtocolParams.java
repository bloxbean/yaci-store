package com.bloxbean.cardano.yaci.store.common.domain;

import com.bloxbean.cardano.yaci.core.model.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.core.model.PoolVotingThresholds;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProtocolParams {
    private Integer minFeeA; //0
    private Integer minFeeB; //1
    private Integer maxBlockSize; //2
    private Integer maxTxSize; //3
    private Integer maxBlockHeaderSize; //4
    private BigInteger keyDeposit; //5
    private BigInteger poolDeposit; //6
    private Integer maxEpoch; //7
    @JsonProperty("nopt")
    private Integer nOpt; //8
    private BigDecimal poolPledgeInfluence; //rational //9
    private BigDecimal expansionRate; //unit interval //10
    private BigDecimal treasuryGrowthRate; //11
    private BigDecimal decentralisationParam; //12
    private String extraEntropy; //13
    private Integer protocolMajorVer; //14
    private Integer protocolMinorVer; //14
    private BigInteger minUtxo; //TODO //15

    private BigInteger minPoolCost; //16
    private BigInteger adaPerUtxoByte; //17
    //private String nonce;

    //Alonzo changes
    private Map<String, long[]> costModels; //18
    private String costModelsHash;

    //ex_unit_prices
    private BigDecimal priceMem; //19
    private BigDecimal priceStep; //19

    //max tx ex units
    private BigInteger maxTxExMem; //20
    private BigInteger maxTxExSteps; //20

    //max block ex units
    private BigInteger maxBlockExMem; //21
    private BigInteger maxBlockExSteps; //21

    private Long maxValSize; //22

    private Integer collateralPercent; //23
    private Integer maxCollateralInputs; //24

//    //Cost per UTxO word for Alonzo.
//    //Cost per UTxO byte for Babbage and later.
//    private String coinsPerUtxoSize;
//    @Deprecated
//    private String coinsPerUtxoWord;

    //Conway era fields
    private PoolVotingThresholds poolVotingThresholds; //25
    private DrepVoteThresholds dRepVoteThresholds; //26
    private Integer minCommitteeSize; //27
    private Integer committeeTermLimit; //28
    private Integer governanceActionValidityPeriod; //29
    private BigInteger governanceActionDeposit; //30
    private BigInteger drepDeposit; //31
    private Integer drepInactivityPeriod; //32

    public void merge(ProtocolParams other) {
        if (other.minFeeA != null) {
            this.minFeeA = other.minFeeA;
        }
        if (other.minFeeB != null) {
            this.minFeeB = other.minFeeB;
        }
        if (other.maxBlockSize != null) {
            this.maxBlockSize = other.maxBlockSize;
        }
        if (other.maxTxSize != null) {
            this.maxTxSize = other.maxTxSize;
        }
        if (other.maxBlockHeaderSize != null) {
            this.maxBlockHeaderSize = other.maxBlockHeaderSize;
        }
        if (other.keyDeposit != null) {
            this.keyDeposit = other.keyDeposit;
        }
        if (other.poolDeposit != null) {
            this.poolDeposit = other.poolDeposit;
        }
        if (other.maxEpoch != null) {
            this.maxEpoch = other.maxEpoch;
        }
        if (other.nOpt != null) {
            this.nOpt = other.nOpt;
        }
        if (other.poolPledgeInfluence != null) {
            this.poolPledgeInfluence = other.poolPledgeInfluence;
        }
        if (other.expansionRate != null) {
            this.expansionRate = other.expansionRate;
        }
        if (other.treasuryGrowthRate != null) {
            this.treasuryGrowthRate = other.treasuryGrowthRate;
        }
        if (other.decentralisationParam != null) {
            this.decentralisationParam = other.decentralisationParam;
        }
        if (other.extraEntropy != null) {
            this.extraEntropy = other.extraEntropy;
        }
        if (other.protocolMajorVer != null) {
            this.protocolMajorVer = other.protocolMajorVer;
        }
        if (other.protocolMinorVer != null) {
            this.protocolMinorVer = other.protocolMinorVer;
        }
        if (other.minUtxo != null) {
            this.minUtxo = other.minUtxo;
        }
        if (other.minPoolCost != null) {
            this.minPoolCost = other.minPoolCost;
        }
        if (other.adaPerUtxoByte != null) {
            this.adaPerUtxoByte = other.adaPerUtxoByte;
        }
        if (other.costModels != null) {
            if (this.costModels == null) {
                this.costModels = other.getCostModels();
            } else {
                var keys = other.getCostModels().keySet();
                keys.forEach(key -> this.costModels.put(key, other.costModels.get(key)));
            }
        }

        if (other.costModelsHash != null) {
            this.costModelsHash = other.costModelsHash;
        }

        if (other.priceMem != null) {
            this.priceMem = other.priceMem;
        }
        if (other.priceStep != null) {
            this.priceStep = other.priceStep;
        }
        if (other.maxTxExMem != null) {
            this.maxTxExMem = other.maxTxExMem;
        }
        if (other.maxTxExSteps != null) {
            this.maxTxExSteps = other.maxTxExSteps;
        }
        if (other.maxBlockExMem != null) {
            this.maxBlockExMem = other.maxBlockExMem;
        }
        if (other.maxBlockExSteps != null) {
            this.maxBlockExSteps = other.maxBlockExSteps;
        }
        if (other.maxValSize != null) {
            this.maxValSize = other.maxValSize;
        }
        if (other.collateralPercent != null) {
            this.collateralPercent = other.collateralPercent;
        }
        if (other.maxCollateralInputs != null) {
            this.maxCollateralInputs = other.maxCollateralInputs;
        }
        if (other.poolVotingThresholds != null) {
            this.poolVotingThresholds = other.poolVotingThresholds;
        }
        if (other.dRepVoteThresholds != null) {
            this.dRepVoteThresholds = other.dRepVoteThresholds;
        }
        if (other.minCommitteeSize != null) {
            this.minCommitteeSize = other.minCommitteeSize;
        }
        if (other.committeeTermLimit != null) {
            this.committeeTermLimit = other.committeeTermLimit;
        }
        if (other.governanceActionValidityPeriod != null) {
            this.governanceActionValidityPeriod = other.governanceActionValidityPeriod;
        }
        if (other.governanceActionDeposit != null) {
            this.governanceActionDeposit = other.governanceActionDeposit;
        }
        if (other.drepDeposit != null) {
            this.drepDeposit = other.drepDeposit;
        }
        if (other.drepInactivityPeriod != null) {
            this.drepInactivityPeriod = other.drepInactivityPeriod;
        }
    }
}
