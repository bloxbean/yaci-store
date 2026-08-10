package com.bloxbean.cardano.yaci.store.common.ccl;

import com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil.safeRatio;

/**
 * Utility methods for converting Yaci Store protocol parameter domain objects to
 * Cardano Client Lib protocol parameter objects.
 */
public final class CclProtocolParamsMapper {
    private CclProtocolParamsMapper() {
    }

    /**
     * Converts a Yaci Store {@link ProtocolParams} instance to the CCL
     * {@link com.bloxbean.cardano.client.api.model.ProtocolParams} model.
     *
     * @param protocolParams protocol parameters from Yaci Store
     * @return the equivalent CCL protocol parameters, or {@code null} when the input is {@code null}
     */
    public static com.bloxbean.cardano.client.api.model.ProtocolParams toCclProtocolParams(ProtocolParams protocolParams) {
        if (protocolParams == null)
            return null;

        var cclProtocolParams = new com.bloxbean.cardano.client.api.model.ProtocolParams();
        cclProtocolParams.setMinFeeA(protocolParams.getMinFeeA());
        cclProtocolParams.setMinFeeB(protocolParams.getMinFeeB());
        cclProtocolParams.setMaxBlockSize(protocolParams.getMaxBlockSize());
        cclProtocolParams.setMaxTxSize(protocolParams.getMaxTxSize());
        cclProtocolParams.setMaxBlockHeaderSize(protocolParams.getMaxBlockHeaderSize());
        cclProtocolParams.setKeyDeposit(toString(protocolParams.getKeyDeposit()));
        cclProtocolParams.setPoolDeposit(toString(protocolParams.getPoolDeposit()));
        cclProtocolParams.setEMax(protocolParams.getMaxEpoch());
        cclProtocolParams.setNOpt(protocolParams.getNOpt());
        cclProtocolParams.setA0(safeRatio(protocolParams.getPoolPledgeInfluence()));
        cclProtocolParams.setRho(safeRatio(protocolParams.getExpansionRate()));
        cclProtocolParams.setTau(safeRatio(protocolParams.getTreasuryGrowthRate()));
        cclProtocolParams.setDecentralisationParam(safeRatio(protocolParams.getDecentralisationParam()));
        cclProtocolParams.setExtraEntropy(protocolParams.getExtraEntropy());
        cclProtocolParams.setProtocolMajorVer(protocolParams.getProtocolMajorVer());
        cclProtocolParams.setProtocolMinorVer(protocolParams.getProtocolMinorVer());
        cclProtocolParams.setMinUtxo(toString(protocolParams.getMinUtxo()));
        cclProtocolParams.setMinPoolCost(toString(protocolParams.getMinPoolCost()));
        cclProtocolParams.setCostModels(costModelsFromRaw(protocolParams.getCostModels()));
        cclProtocolParams.setPriceMem(safeRatio(protocolParams.getPriceMem()));
        cclProtocolParams.setPriceStep(safeRatio(protocolParams.getPriceStep()));
        cclProtocolParams.setMaxTxExMem(toString(protocolParams.getMaxTxExMem()));
        cclProtocolParams.setMaxTxExSteps(toString(protocolParams.getMaxTxExSteps()));
        cclProtocolParams.setMaxBlockExMem(toString(protocolParams.getMaxBlockExMem()));
        cclProtocolParams.setMaxBlockExSteps(toString(protocolParams.getMaxBlockExSteps()));
        cclProtocolParams.setMaxValSize(toString(protocolParams.getMaxValSize()));
        cclProtocolParams.setCollateralPercent(toBigDecimal(protocolParams.getCollateralPercent()));
        cclProtocolParams.setMaxCollateralInputs(protocolParams.getMaxCollateralInputs());
        cclProtocolParams.setCoinsPerUtxoSize(toString(protocolParams.getAdaPerUtxoByte()));
        cclProtocolParams.setCoinsPerUtxoWord(toString(protocolParams.getAdaPerUtxoByte()));

        setVotingThresholds(cclProtocolParams, protocolParams.getPoolVotingThresholds(), protocolParams.getDrepVotingThresholds());

        cclProtocolParams.setCommitteeMinSize(protocolParams.getCommitteeMinSize());
        cclProtocolParams.setCommitteeMaxTermLength(protocolParams.getCommitteeMaxTermLength());
        cclProtocolParams.setGovActionLifetime(protocolParams.getGovActionLifetime());
        cclProtocolParams.setGovActionDeposit(protocolParams.getGovActionDeposit());
        cclProtocolParams.setDrepDeposit(protocolParams.getDrepDeposit());
        cclProtocolParams.setDrepActivity(protocolParams.getDrepActivity());
        cclProtocolParams.setMinFeeRefScriptCostPerByte(safeRatio(protocolParams.getMinFeeRefScriptCostPerByte()));

        return cclProtocolParams;
    }

    /**
     * Converts raw cost model arrays to the CCL map shape. The language names are
     * kept unchanged and array indexes become string keys, preserving cost order.
     *
     * @param costModels raw cost model arrays keyed by language
     * @return CCL-compatible cost model map, or {@code null} when the input is {@code null}
     */
    public static LinkedHashMap<String, LinkedHashMap<String, Long>> costModelsFromRaw(Map<String, long[]> costModels) {
        if (costModels == null)
            return null;

        var result = new LinkedHashMap<String, LinkedHashMap<String, Long>>();
        costModels.forEach((language, costs) -> result.put(language, costArrayToMap(costs)));
        return result;
    }

    /**
     * Converts a JSON object containing raw cost model arrays to the CCL map
     * shape. Non-array language entries are ignored.
     *
     * @param costModelsRawNode JSON object from {@code cost_models_raw}
     * @return CCL-compatible cost model map, or {@code null} when the input is not an object
     */
    public static LinkedHashMap<String, LinkedHashMap<String, Long>> costModelsFromRawJson(JsonNode costModelsRawNode) {
        if (costModelsRawNode == null || !costModelsRawNode.isObject())
            return null;

        var result = new LinkedHashMap<String, LinkedHashMap<String, Long>>();
        Iterator<Map.Entry<String, JsonNode>> fields = costModelsRawNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode costsNode = entry.getValue();
            if (!costsNode.isArray())
                continue;

            var costs = new long[costsNode.size()];
            for (int i = 0; i < costsNode.size(); i++) {
                costs[i] = costsNode.get(i).asLong();
            }
            result.put(entry.getKey(), costArrayToMap(costs));
        }

        return result;
    }

    private static void setVotingThresholds(com.bloxbean.cardano.client.api.model.ProtocolParams cclProtocolParams,
                                            PoolVotingThresholds poolVotingThresholds,
                                            DrepVoteThresholds drepVotingThresholds) {
        if (poolVotingThresholds != null) {
            cclProtocolParams.setPvtMotionNoConfidence(safeRatio(poolVotingThresholds.getPvtMotionNoConfidence()));
            cclProtocolParams.setPvtCommitteeNormal(safeRatio(poolVotingThresholds.getPvtCommitteeNormal()));
            cclProtocolParams.setPvtCommitteeNoConfidence(safeRatio(poolVotingThresholds.getPvtCommitteeNoConfidence()));
            cclProtocolParams.setPvtHardForkInitiation(safeRatio(poolVotingThresholds.getPvtHardForkInitiation()));
            cclProtocolParams.setPvtPPSecurityGroup(safeRatio(poolVotingThresholds.getPvtPPSecurityGroup()));
        }

        if (drepVotingThresholds != null) {
            cclProtocolParams.setDvtMotionNoConfidence(safeRatio(drepVotingThresholds.getDvtMotionNoConfidence()));
            cclProtocolParams.setDvtCommitteeNormal(safeRatio(drepVotingThresholds.getDvtCommitteeNormal()));
            cclProtocolParams.setDvtCommitteeNoConfidence(safeRatio(drepVotingThresholds.getDvtCommitteeNoConfidence()));
            cclProtocolParams.setDvtUpdateToConstitution(safeRatio(drepVotingThresholds.getDvtUpdateToConstitution()));
            cclProtocolParams.setDvtHardForkInitiation(safeRatio(drepVotingThresholds.getDvtHardForkInitiation()));
            cclProtocolParams.setDvtPPNetworkGroup(safeRatio(drepVotingThresholds.getDvtPPNetworkGroup()));
            cclProtocolParams.setDvtPPEconomicGroup(safeRatio(drepVotingThresholds.getDvtPPEconomicGroup()));
            cclProtocolParams.setDvtPPTechnicalGroup(safeRatio(drepVotingThresholds.getDvtPPTechnicalGroup()));
            cclProtocolParams.setDvtPPGovGroup(safeRatio(drepVotingThresholds.getDvtPPGovGroup()));
            cclProtocolParams.setDvtTreasuryWithdrawal(safeRatio(drepVotingThresholds.getDvtTreasuryWithdrawal()));
        }
    }

    private static LinkedHashMap<String, Long> costArrayToMap(long[] costs) {
        var costMap = new LinkedHashMap<String, Long>();
        if (costs == null)
            return costMap;

        for (int i = 0; i < costs.length; i++) {
            costMap.put(String.valueOf(i), costs[i]);
        }

        return costMap;
    }

    private static String toString(BigInteger value) {
        return value != null ? value.toString() : null;
    }

    private static String toString(Long value) {
        return value != null ? value.toString() : null;
    }

    private static BigDecimal toBigDecimal(Integer value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }
}
