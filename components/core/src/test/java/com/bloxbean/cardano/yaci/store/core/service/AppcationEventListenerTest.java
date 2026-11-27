package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.events.internal.RequiredSyncRestartEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppcationEventListenerTest {
    private final StartService startService = mock(StartService.class);
    private final StoreProperties storeProperties = mock(StoreProperties.class);
    private final BlockFetchService blockFetchService = mock(BlockFetchService.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final AppcationEventListener listener = new AppcationEventListener(startService, mock(CursorService.class),
            storeProperties, blockFetchService, publisher);

    @Test
    void aFailedFirstStartIsLeftToAutoRestart() {
        when(storeProperties.isAutoRestartEnabled()).thenReturn(true);
        doThrow(new NodeNotAnsweringException("The node did not answer")).when(startService).start();

        listener.initialize(null);

        verify(blockFetchService).markRestartFailed();
        ArgumentCaptor<RequiredSyncRestartEvent> event = ArgumentCaptor.forClass(RequiredSyncRestartEvent.class);
        verify(publisher).publishEvent(event.capture());
        assertThat(event.getValue().getReason()).isEqualTo("StartFailed");
    }

    @Test
    void withoutAutoRestartAFailedFirstStartStillFailsTheApp() {
        when(storeProperties.isAutoRestartEnabled()).thenReturn(false);
        doThrow(new IllegalStateException("boom")).when(startService).start();

        assertThatThrownBy(() -> listener.initialize(null)).hasMessage("boom");
        verify(blockFetchService, never()).markRestartFailed();
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void aConfigurationErrorStillFailsTheApp() {
        when(storeProperties.isAutoRestartEnabled()).thenReturn(true);
        doThrow(new IllegalStateException("Shelley start slot is not configured for this network")).when(startService).start();

        assertThatThrownBy(() -> listener.initialize(null)).hasMessage("Shelley start slot is not configured for this network");
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void databaseAndNetworkFailuresAreRetried() {
        assertThat(AppcationEventListener.isTransient(
                new RuntimeException(new org.springframework.dao.DataAccessResourceFailureException("db down")))).isTrue();
        assertThat(AppcationEventListener.isTransient(new RuntimeException(new java.net.ConnectException("refused")))).isTrue();
        assertThat(AppcationEventListener.isTransient(
                new RuntimeException(new java.sql.SQLException("connection refused", "08001")))).isTrue();
        assertThat(AppcationEventListener.isTransient(new IllegalArgumentException("bad config"))).isFalse();
        assertThat(AppcationEventListener.isTransient(
                new org.springframework.jdbc.BadSqlGrammarException("q", "select", new java.sql.SQLException("syntax", "42601")))).isFalse();
    }
}
