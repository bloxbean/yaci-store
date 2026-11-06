package com.bloxbean.cardano.yaci.store.core.processor;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import com.bloxbean.cardano.yaci.store.events.internal.RequiredSyncRestartEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequiredRestartProcessorTest {

    private static final long DEBOUNCE_MS = 30_000L;
    private static final int MAX_ATTEMPTS = 3;
    private static final long WINDOW_MS = 600_000L;
    private static final long BASE_NOW = 1_000_000L;

    @Mock
    private StartService startService;
    @Mock
    private StoreProperties storeProperties;

    @InjectMocks
    private RequiredRestartProcessor processor;

    private final RequiredSyncRestartEvent event =
            RequiredSyncRestartEvent.builder().reason("test").build();

    @BeforeEach
    void setUp() {
        lenient().when(storeProperties.getAutoRestartDebounceWindowMs()).thenReturn(DEBOUNCE_MS);
        lenient().when(storeProperties.getAutoRestartMaxAttempts()).thenReturn(MAX_ATTEMPTS);
        lenient().when(storeProperties.getAutoRestartWindowMs()).thenReturn(WINDOW_MS);
    }

    @Test
    void countsUpWhenAttemptsAreSpacedWithinTheWindow() {
        assertThat(processor.evaluateRestartAttempt(BASE_NOW, event)).isEqualTo(1);
        assertThat(processor.evaluateRestartAttempt(BASE_NOW + 60_000, event)).isEqualTo(2);
        assertThat(processor.evaluateRestartAttempt(BASE_NOW + 120_000, event)).isEqualTo(3);
    }

    @Test
    void skipsEventInsideTheDebounceWindow() {
        processor.evaluateRestartAttempt(BASE_NOW, event);

        // 10s later, still inside the 30s debounce window
        assertThat(processor.evaluateRestartAttempt(BASE_NOW + 10_000, event)).isZero();
    }

    @Test
    void pausesOnceMaxAttemptsReachedWithinTheWindow() {
        processor.evaluateRestartAttempt(BASE_NOW, event);
        processor.evaluateRestartAttempt(BASE_NOW + 60_000, event);
        processor.evaluateRestartAttempt(BASE_NOW + 120_000, event); // third hits maxAttempts

        assertThat(processor.evaluateRestartAttempt(BASE_NOW + 180_000, event)).isZero();
    }

    @Test
    void resumesAfterTheWindowElapses() {
        processor.evaluateRestartAttempt(BASE_NOW, event);
        processor.evaluateRestartAttempt(BASE_NOW + 60_000, event);
        long lastAttempt = BASE_NOW + 120_000;
        processor.evaluateRestartAttempt(lastAttempt, event); // budget exhausted
        assertThat(processor.evaluateRestartAttempt(BASE_NOW + 180_000, event)).isZero();

        long afterWindow = lastAttempt + WINDOW_MS + 1;
        assertThat(processor.evaluateRestartAttempt(afterWindow, event)).isEqualTo(1);
    }

    @Test
    void doesNotTouchTheSyncServiceWhileEvaluating() {
        processor.evaluateRestartAttempt(BASE_NOW, event);
        processor.evaluateRestartAttempt(BASE_NOW + 60_000, event);

        verifyNoInteractions(startService);
    }

    @Test
    void keepsRetryingOncePerIntervalAfterMaxAttempts() {
        when(storeProperties.getAutoRestartRetryIntervalAfterMaxMs()).thenReturn(60_000L);
        processor.evaluateRestartAttempt(BASE_NOW, event);
        processor.evaluateRestartAttempt(BASE_NOW + 60_000, event);
        processor.evaluateRestartAttempt(BASE_NOW + 120_000, event); // budget used up

        // 40 s after the last attempt: still too soon
        assertThat(processor.evaluateRestartAttempt(BASE_NOW + 160_000, event)).isZero();
        // 60 s after the last attempt: allowed, without waiting for the 10-minute window
        assertThat(processor.evaluateRestartAttempt(BASE_NOW + 180_000, event)).isEqualTo(4);
        // and again 60 s after that
        assertThat(processor.evaluateRestartAttempt(BASE_NOW + 210_000, event)).isZero();
        assertThat(processor.evaluateRestartAttempt(BASE_NOW + 240_000, event)).isEqualTo(5);
    }

    @Test
    void restartStopsThenStartsWithTheTokenFromItsOwnStop() {
        processor.shutdownWaitMs = 0;
        when(startService.stopForRestart()).thenReturn(7L);
        when(startService.startAfterRestart(7L)).thenReturn(true);

        processor.performRestart(event);

        var order = inOrder(startService);
        order.verify(startService).stopForRestart();
        order.verify(startService).startAfterRestart(7L);
    }

    @Test
    void failedStartPropagatesSoItIsLogged() {
        processor.shutdownWaitMs = 0;
        when(startService.stopForRestart()).thenReturn(1L);
        doThrow(new IllegalStateException("database down")).when(startService).startAfterRestart(1L);

        assertThatThrownBy(() -> processor.performRestart(event)).hasMessage("database down");
    }

    @Test
    void backoffStaysPositiveForLargeAttemptNumbers() {
        when(storeProperties.getAutoRestartBackoffBaseMs()).thenReturn(5_000L);

        assertThat(processor.calculateBackoff(1)).isEqualTo(5_000L);
        assertThat(processor.calculateBackoff(4)).isEqualTo(40_000L);
        // attempts keep counting during a long outage; the backoff must stay at the 1-minute cap
        for (int attempt : new int[]{5, 52, 64, 1_000, Integer.MAX_VALUE}) {
            assertThat(processor.calculateBackoff(attempt)).isEqualTo(60_000L);
        }
    }
}
