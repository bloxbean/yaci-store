package com.bloxbean.cardano.yaci.store.epoch.mapper;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Special;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.util.PlutusKeys;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                BigInteger val = ((UnsignedInteger) di).getValue();
                costs.add(val.longValue());
            }

            long[] costArray = costs.stream()
                    .mapToLong(Long::longValue)
                    .toArray();

            resultMap.put(strKey, costArray);
        }

        return resultMap;
    }
}
