package com.bloxbean.cardano.yaci.store.common.ccl;

import com.bloxbean.cardano.yaci.core.types.NonNegativeInterval;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CclProtocolParamsMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toCclProtocolParamsUsesNumericCostModelKeysToPreserveArrayOrder() {
        ProtocolParams protocolParams = ProtocolParams.builder()
                .minFeeA(44)
                .minFeeB(155381)
                .keyDeposit(BigInteger.valueOf(2_000_000))
                .maxEpoch(18)
                .adaPerUtxoByte(BigInteger.valueOf(4310))
                .priceMem(new NonNegativeInterval(BigInteger.valueOf(577), BigInteger.valueOf(10_000)))
                .costModels(Map.of("PlutusV3", new long[]{100, 200, -1}))
                .build();

        var cclProtocolParams = CclProtocolParamsMapper.toCclProtocolParams(protocolParams);

        assertThat(cclProtocolParams.getMinFeeA()).isEqualTo(44);
        assertThat(cclProtocolParams.getKeyDeposit()).isEqualTo("2000000");
        assertThat(cclProtocolParams.getEMax()).isEqualTo(18);
        assertThat(cclProtocolParams.getCoinsPerUtxoSize()).isEqualTo("4310");
        assertThat(cclProtocolParams.getPriceMem()).isEqualByComparingTo("0.0577");
        assertThat(cclProtocolParams.getCostModels().get("PlutusV3"))
                .containsEntry("0", 100L)
                .containsEntry("1", 200L)
                .containsEntry("2", -1L);
    }

    @Test
    void costModelsFromRawJsonUsesRawCostModelArray() throws Exception {
        var json = objectMapper.readTree("""
                {
                  "PlutusV1": [10, 20],
                  "PlutusV2": [30]
                }
                """);

        var costModels = CclProtocolParamsMapper.costModelsFromRawJson(json);

        assertThat(costModels.get("PlutusV1")).containsEntry("0", 10L).containsEntry("1", 20L);
        assertThat(costModels.get("PlutusV2")).containsEntry("0", 30L);
    }
}
