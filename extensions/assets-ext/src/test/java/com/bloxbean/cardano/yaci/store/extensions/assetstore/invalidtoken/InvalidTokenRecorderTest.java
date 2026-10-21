package com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.storage.impl.model.InvalidToken;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.storage.impl.repository.InvalidTokenRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("InvalidTokenRecorder")
class InvalidTokenRecorderTest {

    private InvalidTokenRepository repository;
    private SimpleMeterRegistry meters;
    private InvalidTokenRecorder recorder;

    @BeforeEach
    void setUp() {
        repository = mock(InvalidTokenRepository.class);
        meters = new SimpleMeterRegistry();
        recorder = new InvalidTokenRecorder(repository, meters);
    }

    private static InvalidTokenReport.InvalidTokenReportBuilder report() {
        return InvalidTokenReport.builder().standard(InvalidTokenReason.CIP_68)
                .reasonCode(InvalidTokenReason.DATUM_UNDECODABLE).reason("cannot decode")
                .policyId("aa".repeat(28)).assetName("000643b0").txHash("bb".repeat(32)).outputIndex(0).slot(10L);
    }

    @Test
    @DisplayName("the same item always gets the same id; a different output or reason gets another")
    void idIsDeterministic() {
        String id = InvalidTokenRecorder.idOf(report().build());

        assertThat(id).hasSize(64).isEqualTo(InvalidTokenRecorder.idOf(report().reason("another text").slot(99L).build()));
        assertThat(id).isNotEqualTo(InvalidTokenRecorder.idOf(report().outputIndex(1).build()));
        assertThat(id).isNotEqualTo(InvalidTokenRecorder.idOf(report().reasonCode(InvalidTokenReason.PROCESSING_FAILURE).build()));
    }

    @Test
    @DisplayName("a new item is saved and counted")
    void savesAndCounts() {
        recorder.report(report().build());

        ArgumentCaptor<InvalidToken> row = ArgumentCaptor.forClass(InvalidToken.class);
        verify(repository).save(row.capture());
        assertThat(row.getValue().getStandard()).isEqualTo("CIP-68");
        assertThat(row.getValue().getFirstSeenAt()).isNotNull();
        assertThat(meters.counter(InvalidTokenRecorder.COUNTER, "standard", "CIP-68", "reason_code", "datum_undecodable").count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("an item that is already recorded is left alone and not counted again")
    void existingItemIsNotSavedAgain() {
        when(repository.existsById(any())).thenReturn(true);

        recorder.report(report().build());

        verify(repository, never()).save(any());
        assertThat(meters.getMeters()).isEmpty();
    }

    @Test
    @DisplayName("text and details are cut to their bounds")
    void boundsAreApplied() {
        Map<String, Object> details = new HashMap<>();
        details.put("long", "x".repeat(5000));

        InvalidToken row = recorder.toRow(report().reason("r".repeat(2000)).subject("s".repeat(1000))
                .origin("o".repeat(2000)).details(details).build());

        assertThat(row.getReason()).hasSize(InvalidTokenRecorder.MAX_REASON_CHARS);
        assertThat(row.getSubject()).hasSize(InvalidTokenRecorder.MAX_SUBJECT_CHARS);
        assertThat(row.getOrigin()).hasSize(InvalidTokenRecorder.MAX_ORIGIN_CHARS);
        assertThat((String) row.getDetails().get("long")).hasSize(InvalidTokenRecorder.MAX_DETAIL_VALUE_CHARS);
    }

    @Test
    @DisplayName("details with too many entries are cut to the limit")
    void tooManyDetailEntries() {
        Map<String, Object> details = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            details.put("k" + i, "v");
        }

        assertThat(InvalidTokenRecorder.boundedDetails(details)).hasSize(InvalidTokenRecorder.MAX_DETAIL_ENTRIES);
    }

    @Test
    @DisplayName("a failing repository is a warning, never an exception")
    void failureIsContained() {
        when(repository.existsById(any())).thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> recorder.report(report().build())).doesNotThrowAnyException();
        assertThatCode(() -> recorder.forgetOrigin("CIP-26", "x.json")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the no-op recorder records nothing and does not throw")
    void noop() {
        assertThatCode(() -> InvalidTokenRecorder.noop().report(report().build())).doesNotThrowAnyException();
    }
}
