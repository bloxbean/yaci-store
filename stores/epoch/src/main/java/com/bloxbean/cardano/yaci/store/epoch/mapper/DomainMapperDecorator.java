package com.bloxbean.cardano.yaci.store.epoch.mapper;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Special;
import co.nstant.in.cbor.model.Number;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.DrepVoteThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.PoolVotingThresholds;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.util.PlutusKeys;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import com.bloxbean.cardano.yaci.store.epoch.util.PlutusOps;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil.safeRatio;

public class DomainMapperDecorator implements DomainMapper {
    private final DomainMapper delegate;

    public DomainMapperDecorator(DomainMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public ProtocolParams toProtocolParams(ProtocolParamUpdate protocolParamUpdate) {
        ProtocolParams protocolParams = delegate.toProtocolParams(protocolParamUpdate);

        //extra entropy
        if (protocolParamUpdate.getExtraEntropy() != null) {
            Integer k = protocolParamUpdate.getExtraEntropy()._1;
            String v = protocolParamUpdate.getExtraEntropy()._2;

            protocolParams.setExtraEntropy(k + "," + v);
        }

        var updatedCostModelMap = getCostModels(protocolParamUpdate);
        if (updatedCostModelMap != null && updatedCostModelMap.size() > 0) {
            protocolParams.setCostModels(updatedCostModelMap);
            protocolParams.setCostModelsHash(protocolParams.getCostModelsHash());
        }

        return protocolParams;
    }

    private Map<String, long[]> getCostModels(ProtocolParamUpdate protocolParamUpdate) {
        var costModelmap = protocolParamUpdate.getCostModels();
        if (costModelmap == null)
            return null;

        Map<String, long[]> resultMap = new HashMap<>();
        var languageKeys = costModelmap.keySet();
        for (var key : languageKeys) {
            String strKey = null;
            if (key == 0) {
                strKey = PlutusKeys.PLUTUS_V1;
            } else if (key == 1) {
                strKey = PlutusKeys.PLUTUS_V2;
            } else if (key == 2) {
                strKey = PlutusKeys.PLUTUS_V3;
            }

            var cborCostModelValue = costModelmap.get(key);
            Array array = (Array) CborSerializationUtil.deserializeOne(HexUtil.decodeHexString(cborCostModelValue));
            List<Long> costs = new ArrayList<>();
            for (DataItem di : array.getDataItems()) {
                if (di == Special.BREAK)
                    continue;
                BigInteger val = ((Number) di).getValue();
                costs.add(val.longValue());
            }

            long[] costArray = costs.stream()
                    .mapToLong(Long::longValue)
                    .toArray();

            resultMap.put(strKey, costArray);
        }

        return resultMap;
    }

    @Override
    public ProtocolParamsDto toProtocolParamsDto(ProtocolParams protocolParams) {
        var protocolParamsDto = delegate.toProtocolParamsDto(protocolParams);

        var costModelMap = protocolParams.getCostModels();
        if (costModelMap != null) {
            if (protocolParamsDto.getCostModels() == null)
                protocolParamsDto.setCostModels(new TreeMap<>());

            for (var key : costModelMap.keySet()) {
                List<String> ops = switch (key) {
                    case PlutusKeys.PLUTUS_V1 -> PlutusOps.getOperations(1);
                    case PlutusKeys.PLUTUS_V2 -> PlutusOps.getOperations(2);
                    case PlutusKeys.PLUTUS_V3 -> PlutusOps.getOperations(3);
                    default -> Collections.emptyList();
                };

                Map<String, Long> langCost = new TreeMap<>();
                var costArr = costModelMap.get(key);
                if (costArr.length == ops.size()) {
                    int index = 0;

                    for (String op : ops) {
                        langCost.put(op, costArr[index++]);
                    }
                } else {
                    int index = 0;
                    for (var opCost : costArr) {
                        langCost.put(String.format("%03d", index++), opCost);
                    }
                }

                protocolParamsDto.getCostModels().put(key, langCost);
            }
        }



        //UnitInterval and NonNegativeInterval
        protocolParamsDto.setA0(safeRatio(protocolParams.getPoolPledgeInfluence()));
        protocolParamsDto.setRho(safeRatio(protocolParams.getExpansionRate()));
        protocolParamsDto.setTau(safeRatio(protocolParams.getTreasuryGrowthRate()));
        protocolParamsDto.setDecentralisationParam(safeRatio(protocolParams.getDecentralisationParam()));
        protocolParamsDto.setPriceMem(safeRatio(protocolParams.getPriceMem()));
        protocolParamsDto.setPriceStep(safeRatio(protocolParams.getPriceStep()));

        //pvt
        PoolVotingThresholds poolVotingThresholds = protocolParams.getPoolVotingThresholds();
        if (poolVotingThresholds != null) {
            protocolParamsDto.setPvtMotionNoConfidence(safeRatio(poolVotingThresholds.getPvtMotionNoConfidence()));
            protocolParamsDto.setPvtCommitteeNormal(safeRatio(poolVotingThresholds.getPvtCommitteeNormal()));
            protocolParamsDto.setPvtCommitteeNoConfidence(safeRatio(poolVotingThresholds.getPvtCommitteeNoConfidence()));
            protocolParamsDto.setPvtHardForkInitiation(safeRatio(poolVotingThresholds.getPvtHardForkInitiation()));
            protocolParamsDto.setPvtPPSecurityGroup(safeRatio(poolVotingThresholds.getPvtPPSecurityGroup()));
        }

        //dvt
        DrepVoteThresholds drepVotingThresholds = protocolParams.getDrepVotingThresholds();

        if (drepVotingThresholds != null) {
            protocolParamsDto.setDvtMotionNoConfidence(safeRatio(drepVotingThresholds.getDvtMotionNoConfidence()));
            protocolParamsDto.setDvtCommitteeNormal(safeRatio(drepVotingThresholds.getDvtCommitteeNormal()));
            protocolParamsDto.setDvtCommitteeNoConfidence(safeRatio(drepVotingThresholds.getDvtCommitteeNoConfidence()));
            protocolParamsDto.setDvtUpdateToConstitution(safeRatio(drepVotingThresholds.getDvtUpdateToConstitution()));
            protocolParamsDto.setDvtHardForkInitiation(safeRatio(drepVotingThresholds.getDvtHardForkInitiation()));
            protocolParamsDto.setDvtPPNetworkGroup(safeRatio(drepVotingThresholds.getDvtPPNetworkGroup()));
            protocolParamsDto.setDvtPPEconomicGroup(safeRatio(drepVotingThresholds.getDvtPPEconomicGroup()));
            protocolParamsDto.setDvtPPTechnicalGroup(safeRatio(drepVotingThresholds.getDvtPPTechnicalGroup()));
            protocolParamsDto.setDvtPPGovGroup(safeRatio(drepVotingThresholds.getDvtPPGovGroup()));
            protocolParamsDto.setDvtTreasuryWithdrawal(safeRatio(drepVotingThresholds.getDvtTreasuryWithdrawal()));
        }

        protocolParamsDto.setMinFeeRefScriptCostPerByte(safeRatio(protocolParams.getMinFeeRefScriptCostPerByte()));

        return protocolParamsDto;
    }
}
