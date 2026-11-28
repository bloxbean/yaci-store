package com.bloxbean.cardano.yaci.store.common.mapper;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Special;
import co.nstant.in.cbor.model.Number;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.util.PlutusKeys;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.math.BigInteger;
import java.util.*;

@Mapper(builder = @Builder(disableBuilder = true))
public interface ProtocolParamUpdateMapper {
    ProtocolParamUpdateMapper INSTANCE = Mappers.getMapper(ProtocolParamUpdateMapper.class);

    @Mapping(target = "costModels", ignore = true)
    @Mapping(target = "extraEntropy", ignore = true)
    @Mapping(target = "NOpt", source = "NOpt")
    ProtocolParams toProtocolParams(ProtocolParamUpdate update);

    @AfterMapping
    default void mapSpecialFields(ProtocolParamUpdate protocolParamUpdate, @MappingTarget ProtocolParams protocolParams) {

        //extra entropy
        if (protocolParamUpdate.getExtraEntropy() != null) {
            Integer k = protocolParamUpdate.getExtraEntropy()._1;
            String v = protocolParamUpdate.getExtraEntropy()._2;

            protocolParams.setExtraEntropy(k + "," + v);
        }

        var updatedCostModelMap = getCostModels(protocolParamUpdate);
        if (updatedCostModelMap != null && updatedCostModelMap.size() > 0) {
            protocolParams.setCostModels(updatedCostModelMap);
        }
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

}
