package com.bloxbean.cardano.yaci.store.submit.controller;

import com.bloxbean.cardano.yaci.store.submit.service.TxEvaluationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TxUtilControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void evaluateUtxosReturnsBadRequestForInvalidAdditionalUtxos() throws Exception {
        TxEvaluationService txEvaluationService = mock(TxEvaluationService.class);
        when(txEvaluationService.evaluateTx(any(), any()))
                .thenThrow(new IllegalArgumentException("additionalUtxoSet TxOut value is required"));
        TxUtilController controller = new TxUtilController(txEvaluationService);
        TxUtilController.EvaluateRequest request = new TxUtilController.EvaluateRequest();
        request.setCbor("abcd");
        request.setAdditionalUtxoSet(objectMapper.readTree("[]"));

        var response = controller.evaluateTx(request, 5);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isInstanceOfSatisfying(TxErrorResponse.class, error -> {
            assertThat(error.error()).isEqualTo("Bad Request");
            assertThat(error.message()).contains("additionalUtxoSet TxOut value is required");
        });
    }

    @Test
    void evaluateUtxosPassesAdditionalUtxosAsRawJsonForModeAwareHandling() throws Exception {
        TxEvaluationService txEvaluationService = mock(TxEvaluationService.class);
        JsonNode success = objectMapper.readTree("{\"result\":[]}");
        when(txEvaluationService.evaluateTx(any(), any())).thenReturn(Either.right(success));
        when(txEvaluationService.transformTxEvaluationSuccessResultToV5BFFormat(success)).thenReturn(success);
        TxUtilController controller = new TxUtilController(txEvaluationService);
        TxUtilController.EvaluateRequest request = new TxUtilController.EvaluateRequest();
        request.setCbor("abcd");
        request.setAdditionalUtxoSet(objectMapper.readTree("\"ignored by ogmios mode\""));

        var response = controller.evaluateTx(request, 5);

        ArgumentCaptor<JsonNode> additionalUtxoCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(txEvaluationService).evaluateTx(any(), additionalUtxoCaptor.capture());
        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(additionalUtxoCaptor.getValue().asText()).isEqualTo("ignored by ogmios mode");
    }
}
