package com.bloxbean.cardano.yaci.store.epoch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
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
    //Blockfrost compatibility: defaults to 0 when not set (see DomainMapperDecorator), as Blockfrost returns it for every era
    private BigDecimal decentralisationParam;
    //Blockfrost compatibility: Blockfrost always includes extra_entropy (null when not set) and some clients
    //(e.g. PyCardano) fail if the field is missing, so it is excluded from the class level NON_NULL
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String extraEntropy;
    private Integer protocolMajorVer;
    private Integer protocolMinorVer;
    //Blockfrost compatibility: defaults to coins_per_utxo_size when not set (see DomainMapperDecorator)
    private String minUtxo;
    private String minPoolCost;
    private String nonce;

    //Alonzo changes
    private Map<String, Map<String, Long>> costModels;
    @JsonProperty("cost_models_raw")
    private Map<String, List<Long>> costModelsRaw;
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
    //Blockfrost compatibility: defaults to coins_per_utxo_size when not set (see DomainMapperDecorator)
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
    //Blockfrost compatibility: Blockfrost field names (dvt_p_p_*). The default snake case naming would give dvt_pp*
    @JsonProperty("dvt_p_p_network_group")
    private BigDecimal dvtPPNetworkGroup;
    @JsonProperty("dvt_p_p_economic_group")
    private BigDecimal dvtPPEconomicGroup;
    @JsonProperty("dvt_p_p_technical_group")
    private BigDecimal dvtPPTechnicalGroup;
    @JsonProperty("dvt_p_p_gov_group")
    private BigDecimal dvtPPGovGroup;
    private BigDecimal dvtTreasuryWithdrawal;

    //Blockfrost compatibility: Blockfrost returns these as strings (like key_deposit and pool_deposit), and clients
    //that validate the response against Blockfrost's schema (e.g. Evolution SDK) reject numbers. The Java types are
    //unchanged so existing library users are not affected.
    @JsonSerialize(using = ToStringSerializer.class)
    private Integer committeeMinSize;
    @JsonSerialize(using = ToStringSerializer.class)
    private Integer committeeMaxTermLength;
    @JsonSerialize(using = ToStringSerializer.class)
    private Integer govActionLifetime;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigInteger govActionDeposit;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigInteger drepDeposit;
    @JsonSerialize(using = ToStringSerializer.class)
    private Integer drepActivity;
    private BigDecimal minFeeRefScriptCostPerByte;

    //To align with Blockfrost
    @JsonProperty("pvtpp_security_group")
    public BigDecimal getPvtppSecurityGroup() {
        return pvtPPSecurityGroup;
    }
}
