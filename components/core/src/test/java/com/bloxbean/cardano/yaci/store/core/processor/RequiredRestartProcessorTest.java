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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

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
}
