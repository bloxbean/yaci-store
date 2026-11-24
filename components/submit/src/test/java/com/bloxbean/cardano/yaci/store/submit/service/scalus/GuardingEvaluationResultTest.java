package com.bloxbean.cardano.yaci.store.submit.service.scalus;

import com.bloxbean.cardano.client.api.model.EvaluationResult;
import com.bloxbean.cardano.client.plutus.spec.ExUnits;
import com.bloxbean.cardano.client.plutus.spec.RedeemerTag;
import com.bloxbean.cardano.yaci.store.submit.service.TxEvaluationProperties;
import com.bloxbean.cardano.yaci.store.submit.service.TxEvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GuardingEvaluationResultTest {
    @Test
    void exposesTheOgmiosGuardPurposeAndBudgetInBothFormats() {
        var mapper = new ObjectMapper();
        var scalus = new ScalusTxEvaluationService(null, null, null, mapper);
        var guard = EvaluationResult.builder()
                .redeemerTag(RedeemerTag.Guarding)
                .index(2)
                .exUnits(ExUnits.builder().mem(BigInteger.TEN).steps(BigInteger.valueOf(20)).build())
                .build();

        var response = scalus.toJson(List.of(guard));
        assertThat(response.at("/result/0/validator/purpose").asText()).isEqualTo("guard");
        var v5 = new TxEvaluationService(new TxEvaluationProperties(), null, null, mapper)
                .transformTxEvaluationSuccessResultToV5BFFormat(response);
        assertThat(v5.at("/result/EvaluationResult/guard:2/memory").asLong()).isEqualTo(10);
        assertThat(v5.at("/result/EvaluationResult/guard:2/steps").asLong()).isEqualTo(20);
    }
}
