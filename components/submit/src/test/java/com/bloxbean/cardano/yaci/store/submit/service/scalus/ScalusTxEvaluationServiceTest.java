package com.bloxbean.cardano.yaci.store.submit.service.scalus;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.EvaluationResult;
import com.bloxbean.cardano.client.plutus.spec.ExUnits;
import com.bloxbean.cardano.client.plutus.spec.RedeemerTag;
import com.bloxbean.cardano.yaci.store.client.epoch.EpochParamClient;
import com.bloxbean.cardano.yaci.store.client.utxo.DummyUtxoClient;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.submit.service.TxEvaluationProperties;
import com.bloxbean.cardano.yaci.store.submit.service.TxEvaluationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScalusTxEvaluationServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toJsonUsesOgmiosPurposeNamesAndTransformsToV5Shape() {
        ScalusTxEvaluationService scalusService = new ScalusTxEvaluationService(null, null, null, objectMapper);
        JsonNode scalusJson = scalusService.toJson(List.of(
                result(RedeemerTag.Spend, 0, 100, 200),
                result(RedeemerTag.Mint, 1, 101, 201),
                result(RedeemerTag.Cert, 2, 102, 202),
                result(RedeemerTag.Reward, 3, 103, 203),
                result(RedeemerTag.Voting, 4, 104, 204),
                result(RedeemerTag.Proposing, 5, 105, 205)
        ));

        JsonNode v5Json = new TxEvaluationService(new TxEvaluationProperties(), null, null, objectMapper)
                .transformTxEvaluationSuccessResultToV5BFFormat(scalusJson);
        JsonNode evaluationResults = v5Json.at("/result/EvaluationResult");

        assertCost(evaluationResults, "spend:0", 100, 200);
        assertCost(evaluationResults, "mint:1", 101, 201);
        assertCost(evaluationResults, "publish:2", 102, 202);
        assertCost(evaluationResults, "withdraw:3", 103, 203);
        assertCost(evaluationResults, "vote:4", 104, 204);
        assertCost(evaluationResults, "propose:5", 105, 205);
        assertThat(evaluationResults.has("cert:2")).isFalse();
        assertThat(evaluationResults.has("reward:3")).isFalse();
        assertThat(evaluationResults.has("voting:4")).isFalse();
        assertThat(evaluationResults.has("proposing:5")).isFalse();
    }

    @Test
    void evaluateTxRejectsDummyUtxoClient() {
        ObjectProvider<EpochParamClient> epochParamClient = mock(ObjectProvider.class);
        ObjectProvider<UtxoClient> utxoClient = mock(ObjectProvider.class);
        when(epochParamClient.getIfAvailable()).thenReturn(mock(EpochParamClient.class));
        when(utxoClient.getIfAvailable()).thenReturn(new DummyUtxoClient());

        ScalusTxEvaluationService scalusService = new ScalusTxEvaluationService(
                epochParamClient, utxoClient, mock(ScalusSlotConfigProvider.class), objectMapper);

        assertThatThrownBy(() -> scalusService.evaluateTx(new byte[]{1}, objectMapper.readTree("[]")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("real UtxoClient");
    }

    private EvaluationResult result(RedeemerTag redeemerTag, int index, long memory, long steps) {
        return EvaluationResult.builder()
                .redeemerTag(redeemerTag)
                .index(index)
                .exUnits(ExUnits.builder()
                        .mem(BigInteger.valueOf(memory))
                        .steps(BigInteger.valueOf(steps))
                        .build())
                .build();
    }

    private void assertCost(JsonNode evaluationResults, String key, long memory, long steps) {
        JsonNode cost = evaluationResults.get(key);
        assertThat(cost).isNotNull();
        assertThat(cost.get("memory").asLong()).isEqualTo(memory);
        assertThat(cost.get("steps").asLong()).isEqualTo(steps);
    }
}
