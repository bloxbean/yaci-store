package com.bloxbean.cardano.yaci.store.epoch.mapper;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Special;
import co.nstant.in.cbor.model.Number;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.util.PlutusKeys;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import com.bloxbean.cardano.yaci.store.epoch.util.PlutusOps;

import java.math.BigInteger;
import java.util.*;

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
                    case PlutusKeys.PLUTUS_V3 -> Collections.emptyList(); //TODO
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

        //pvt
        protocolParamsDto.setPvtMotionNoConfidence(safeRatio(protocolParams.getPoolVotingThresholds().getPvtMotionNoConfidence()));
        protocolParamsDto.setPvtCommitteeNormal(safeRatio(protocolParams.getPoolVotingThresholds().getPvtCommitteeNormal()));
        protocolParamsDto.setPvtCommitteeNoConfidence(safeRatio(protocolParams.getPoolVotingThresholds().getPvtCommitteeNoConfidence()));
        protocolParamsDto.setPvtHardForkInitiation(safeRatio(protocolParams.getPoolVotingThresholds().getPvtHardForkInitiation()));
        protocolParamsDto.setPvtPPSecurityGroup(safeRatio(protocolParams.getPoolVotingThresholds().getPvtPPSecurityGroup()));

        //dvt
        protocolParamsDto.setDvtMotionNoConfidence(safeRatio(protocolParams.getDrepVotingThresholds().getDvtMotionNoConfidence()));
        protocolParamsDto.setDvtCommitteeNormal(safeRatio(protocolParams.getDrepVotingThresholds().getDvtCommitteeNormal()));
        protocolParamsDto.setDvtCommitteeNoConfidence(safeRatio(protocolParams.getDrepVotingThresholds().getDvtCommitteeNoConfidence()));
        protocolParamsDto.setDvtUpdateToConstitution(safeRatio(protocolParams.getDrepVotingThresholds().getDvtUpdateToConstitution()));
        protocolParamsDto.setDvtHardForkInitiation(safeRatio(protocolParams.getDrepVotingThresholds().getDvtHardForkInitiation()));
        protocolParamsDto.setDvtPPNetworkGroup(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPNetworkGroup()));
        protocolParamsDto.setDvtPPEconomicGroup(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPEconomicGroup()));
        protocolParamsDto.setDvtPPTechnicalGroup(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPTechnicalGroup()));
        protocolParamsDto.setDvtPPGovGroup(safeRatio(protocolParams.getDrepVotingThresholds().getDvtPPGovGroup()));
        protocolParamsDto.setDvtTreasuryWithdrawal(safeRatio(protocolParams.getDrepVotingThresholds().getDvtTreasuryWithdrawal()));

        protocolParamsDto.setMinFeeRefScriptCostPerByte(safeRatio(protocolParams.getMinFeeRefScriptCostPerByte()));

        return protocolParamsDto;
    }
}
