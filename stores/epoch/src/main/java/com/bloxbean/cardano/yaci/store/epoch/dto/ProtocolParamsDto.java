package com.bloxbean.cardano.yaci.store.epoch.dto;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProtocolParamsDto {
    private Integer minFeeA;
    private Integer minFeeB;
    private Integer maxBlockSize;
    private Integer maxTxSize;
    private Integer maxBlockHeaderSize;
    private String keyDeposit;
    private String poolDeposit;
    @JsonProperty("e_max")
    private Integer eMax;
    @JsonProperty("n_opt")
    private Integer nOpt;
    private BigDecimal a0;
    private BigDecimal rho;
    private BigDecimal tau;
    private BigDecimal decentralisationParam;
    private String extraEntropy;
    private Integer protocolMajorVer;
    private Integer protocolMinorVer;
    private String minUtxo;
    private String minPoolCost;
    private String nonce;

    //Alonzo changes
    private Map<String, Map<String, Long>> costModels;
    private BigDecimal priceMem;
    private BigDecimal priceStep;
    private String maxTxExMem;
    private String maxTxExSteps;
    private String maxBlockExMem;
    private String maxBlockExSteps;
    private String maxValSize;
    private BigDecimal collateralPercent;
    private Integer maxCollateralInputs;

    //Cost per UTxO word for Alonzo.
    //Cost per UTxO byte for Babbage and later.
    private String coinsPerUtxoSize;
    @Deprecated
    private String coinsPerUtxoWord;

    //Conway era
    private BigDecimal pvtMotionNoConfidence;
    private BigDecimal pvtCommitteeNormal;
    private BigDecimal pvtCommitteeNoConfidence;
    private BigDecimal pvtHardForkInitiation;
    @JsonProperty("pvt_p_p_security_group")
    private BigDecimal pvtPPSecurityGroup;

    private BigDecimal dvtMotionNoConfidence;
    private BigDecimal dvtCommitteeNormal;
    private BigDecimal dvtCommitteeNoConfidence;
    private BigDecimal dvtUpdateToConstitution;
    private BigDecimal dvtHardForkInitiation;
    private BigDecimal dvtPPNetworkGroup;
    private BigDecimal dvtPPEconomicGroup;
    private BigDecimal dvtPPTechnicalGroup;
    private BigDecimal dvtPPGovGroup;
    private BigDecimal dvtTreasuryWithdrawal;

    private Integer committeeMinSize;
    private Integer committeeMaxTermLength;
    private Integer govActionLifetime;
    private BigInteger govActionDeposit;
    private BigInteger drepDeposit;
    private Integer drepActivity;
    private BigDecimal minFeeRefScriptCostPerByte;

    //To align with Blockfrost
    @JsonProperty("pvtpp_security_group")
    public BigDecimal getPvtppSecurityGroup() {
        return pvtPPSecurityGroup;
    }
}
