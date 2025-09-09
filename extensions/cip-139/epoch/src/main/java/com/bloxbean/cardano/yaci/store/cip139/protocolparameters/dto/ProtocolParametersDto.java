package com.bloxbean.cardano.yaci.store.cip139.protocolparameters.dto;


import com.bloxbean.cardano.yaci.core.types.NonNegativeInterval;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProtocolParametersDto {

    private static final String COST_MODEL_PLUTUS_V1 = "plutus_v1";
    private static final String COST_MODEL_PLUTUS_V2 = "plutus_v2";
    private static final String COST_MODEL_PLUTUS_V3 = "plutus_v3";
    private static final List<String> COST_MODEL_LIST = List.of(COST_MODEL_PLUTUS_V1, COST_MODEL_PLUTUS_V2, COST_MODEL_PLUTUS_V3);

    private String adaPerUtxoByte;
    private Integer collateralPercentage;
    private CostModels costModels;
    private UnitInterval d;
    private ExecutionCosts executionCosts;
    private UnitInterval expansionRate;


    private String keyDeposit;
    private Integer maxBlockBodySize;
    private Unit maxBlockExUnits;
    private Integer maxBlockHeaderSize;
    private Integer maxCollateralInputs;

    private Integer maxEpoch;
    private Unit maxTxExUnits;
    private Integer maxTxSize;
    private Integer maxValueSize;
    private String minPoolCost;
    private String minfeeA;
    private String minfeeB;
    private String nOpt;
    private String poolDeposit;
    private UnitInterval poolPledgeInfluence;
    private ProtocolVersion protocolVersion;

    private UnitInterval treasuryGrowthRate;
    private Threshold poolVotingThresholds;
    private Threshold drepVotingThresholds;
    private String committeeMinSize;
    private Integer committeeMaxTermLength;
    private Integer govActionLifetime;
    private String govActionDeposit;
    private String drepDeposit;
    private Integer drepActivity;
    private UnitInterval minFeeRefScriptCostPerByte;

    private record ExecutionCosts(
            NonNegativeInterval memPrice,
            NonNegativeInterval stepPrice
    ) {}

    private record Unit(
            String mem,
            String steps
    ) {}

    private record ProtocolVersion(
            Integer major,
            Integer minor
    ) {}

    private record Threshold(List<UnitInterval> items) {
        public Threshold(List<UnitInterval> items) {
            this.items = items != null ? List.copyOf(items) : List.of();
        }
    }

    private record CostModels(Map<String, List<String>> costModels) {

    }

    public static ProtocolParametersDto fromDomain(ProtocolParams protocolParams){

        CostModels costModels = new CostModels(createCostModelsMap(protocolParams.getCostModels()));

        ExecutionCosts executionCosts = new ExecutionCosts(protocolParams.getPriceMem(),
                protocolParams.getPriceStep());

        Unit maxBlockExUnits = new Unit(String.valueOf(protocolParams.getMaxBlockExMem()),
                String.valueOf(protocolParams.getMaxBlockExSteps()));

        Unit maxTxExUnits = new Unit(String.valueOf(protocolParams.getMaxTxExMem()),
                String.valueOf(protocolParams.getMaxTxExSteps()));

        ProtocolVersion protocolVersion = new ProtocolVersion(protocolParams.getProtocolMajorVer(),
                protocolParams.getProtocolMinorVer());

        Threshold poolVotingThresholds = new Threshold(createPoolVotingThresholdsList(protocolParams.getPoolVotingThresholds()));

        Threshold drepVotingThresholds = new Threshold(createDrepVotingThresholdsList(protocolParams.getDrepVotingThresholds()));

        return ProtocolParametersDto.builder()
                .adaPerUtxoByte(String.valueOf(protocolParams.getAdaPerUtxoByte()))
                .collateralPercentage(protocolParams.getCollateralPercent())
                .costModels(costModels)
                .d(protocolParams.getDecentralisationParam())
                .executionCosts(executionCosts)
                .expansionRate(protocolParams.getExpansionRate())
                .keyDeposit(String.valueOf(protocolParams.getKeyDeposit()))
                .maxBlockBodySize(protocolParams.getMaxBlockSize())
                .maxBlockExUnits(maxBlockExUnits)
                .maxBlockHeaderSize(protocolParams.getMaxBlockHeaderSize())
                .maxCollateralInputs(protocolParams.getMaxCollateralInputs())
                .maxEpoch(protocolParams.getMaxEpoch())
                .maxTxExUnits(maxTxExUnits)
                .maxTxSize(protocolParams.getMaxTxSize())
                .maxValueSize(Math.toIntExact(protocolParams.getMaxValSize()))
                .minPoolCost(String.valueOf(protocolParams.getMinPoolCost()))
                .minfeeA(String.valueOf(protocolParams.getMinFeeA()))
                .minfeeB(String.valueOf(protocolParams.getMinFeeB()))
                .nOpt(String.valueOf(protocolParams.getNOpt()))
                .poolDeposit(String.valueOf(protocolParams.getPoolDeposit()))
                .poolPledgeInfluence(protocolParams.getPoolPledgeInfluence())
                .protocolVersion(protocolVersion)
                .treasuryGrowthRate(protocolParams.getTreasuryGrowthRate())
                .poolVotingThresholds(poolVotingThresholds)
                .drepVotingThresholds(drepVotingThresholds)
                .committeeMinSize(String.valueOf(protocolParams.getCommitteeMinSize()))
                .govActionLifetime(protocolParams.getGovActionLifetime())
                .govActionDeposit(String.valueOf(protocolParams.getGovActionDeposit()))
                .drepDeposit(String.valueOf(protocolParams.getDrepDeposit()))
                .drepActivity(protocolParams.getDrepActivity())
                .minFeeRefScriptCostPerByte(protocolParams.getMinFeeRefScriptCostPerByte())
                .build();
    }

    private static Map<String, List<String>> createCostModelsMap(Map<String,long[]> costModels) {
        Map<String, List<String>> costModelsMap = new HashMap<>();
        for (String costModel: COST_MODEL_LIST) {
            long[] costModelLongArray = costModels.get(costModel);
            List<String> costModelStringList = Arrays.stream(costModelLongArray)
                    .mapToObj(String::valueOf)
                    .toList();
            costModelsMap.put(costModel, costModelStringList);
        }
        return costModelsMap;
    }

    private static List<UnitInterval> createDrepVotingThresholdsList(DrepVoteThresholds drepVoteThresholds) {
        List<UnitInterval> items = new ArrayList<>();
        items.add(drepVoteThresholds.getDvtMotionNoConfidence());
        items.add(drepVoteThresholds.getDvtCommitteeNormal());
        items.add(drepVoteThresholds.getDvtCommitteeNoConfidence());
        items.add(drepVoteThresholds.getDvtUpdateToConstitution());
        items.add(drepVoteThresholds.getDvtHardForkInitiation());
        items.add(drepVoteThresholds.getDvtPPNetworkGroup());
        items.add(drepVoteThresholds.getDvtPPEconomicGroup());
        items.add(drepVoteThresholds.getDvtPPTechnicalGroup());
        items.add(drepVoteThresholds.getDvtPPGovGroup());
        items.add(drepVoteThresholds.getDvtTreasuryWithdrawal());
        return items;
    }

    private static List<UnitInterval> createPoolVotingThresholdsList(PoolVotingThresholds poolVotingThresholds) {
        List<UnitInterval> items = new ArrayList<>();
        items.add(poolVotingThresholds.getPvtMotionNoConfidence());
        items.add(poolVotingThresholds.getPvtCommitteeNormal());
        items.add(poolVotingThresholds.getPvtCommitteeNoConfidence());
        items.add(poolVotingThresholds.getPvtHardForkInitiation());
        items.add(poolVotingThresholds.getPvtPPSecurityGroup());
        return items;
    }

    public static void main(String[] args) throws JsonProcessingException {
        ProtocolParams protocolParams = new ProtocolParams();
        ProtocolParametersDto protocolParametersDto = ProtocolParametersDto.fromDomain(protocolParams);
        System.out.println(new ObjectMapper().writeValueAsString(protocolParametersDto));
    }
}
