package com.bloxbean.cardano.yaci.store.submit.service;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.yaci.store.submit.service.scalus.ScalusTxEvaluationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TxEvaluationServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ObjectProvider<OgmiosService> ogmiosServiceProvider;
    @Mock
    private OgmiosService ogmiosService;
    @Mock
    private ScalusTxEvaluationService scalusTxEvaluationService;

    private JsonNode success;

    @BeforeEach
    void setup() throws Exception {
        success = objectMapper.readTree("{\"result\":[]}");
    }

    @Test
    void defaultModeUsesScalusEvaluator() throws Exception {
        byte[] cborTx = new byte[]{1, 2, 3};
        JsonNode additionalUtxos = objectMapper.readTree("[]");
        TxEvaluationProperties properties = new TxEvaluationProperties();
        when(scalusTxEvaluationService.evaluateTx(same(cborTx), same(additionalUtxos))).thenReturn(Either.right(success));

        Either<JsonNode, JsonNode> result = service(properties).evaluateTx(cborTx, additionalUtxos);

        assertThat(result.get()).isSameAs(success);
        verify(scalusTxEvaluationService).evaluateTx(same(cborTx), same(additionalUtxos));
        verify(ogmiosServiceProvider, never()).getIfAvailable();
        verifyNoInteractions(ogmiosService);
    }

    @Test
    void ogmiosModeUsesOgmiosEvaluator() throws Exception {
        byte[] cborTx = new byte[]{1, 2, 3};
        TxEvaluationProperties properties = new TxEvaluationProperties();
        properties.setTxEvaluatorMode(TxEvaluatorMode.OGMIOS);
        when(ogmiosServiceProvider.getIfAvailable()).thenReturn(ogmiosService);
        when(ogmiosService.evaluateTx(same(cborTx), anySet())).thenReturn(Either.right(success));

        Either<JsonNode, JsonNode> result = service(properties).evaluateTx(cborTx, objectMapper.readTree("[]"));

        assertThat(result.get()).isSameAs(success);
        verify(ogmiosService).evaluateTx(same(cborTx), anySet());
        verifyNoInteractions(scalusTxEvaluationService);
    }

    @Test
    void scalusModeUsesScalusEvaluatorEvenWhenOgmiosExists() throws Exception {
        byte[] cborTx = new byte[]{1, 2, 3};
        JsonNode additionalUtxos = objectMapper.readTree("[]");
        TxEvaluationProperties properties = new TxEvaluationProperties();
        properties.setTxEvaluatorMode(TxEvaluatorMode.SCALUS);
        when(scalusTxEvaluationService.evaluateTx(same(cborTx), same(additionalUtxos))).thenReturn(Either.right(success));

        Either<JsonNode, JsonNode> result = service(properties).evaluateTx(cborTx, additionalUtxos);

        assertThat(result.get()).isSameAs(success);
        verify(ogmiosServiceProvider, never()).getIfAvailable();
        verify(scalusTxEvaluationService).evaluateTx(same(cborTx), same(additionalUtxos));
    }

    @Test
    void ogmiosModeFailsClearlyWhenOgmiosBeanIsMissing() {
        TxEvaluationProperties properties = new TxEvaluationProperties();
        properties.setTxEvaluatorMode(TxEvaluatorMode.OGMIOS);
        when(ogmiosServiceProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service(properties).evaluateTx(new byte[]{1}, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Ogmios tx evaluator requested")
                .hasMessageContaining("store.cardano.ogmios-url");
    }

    private TxEvaluationService service(TxEvaluationProperties properties) {
        return new TxEvaluationService(properties, ogmiosServiceProvider, scalusTxEvaluationService, objectMapper);
    }
}
