package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.helper.GenesisBlockFinder;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StartServiceRestartTest {
    private BlockFetchService blockFetchService;
    private CursorService cursorService;
    private StartService startService;

    @BeforeEach
    void setUp() {
        blockFetchService = mock(BlockFetchService.class);
        cursorService = mock(CursorService.class);
        startService = new StartService(mock(ApplicationEventPublisher.class), blockFetchService,
                mock(TipFinderService.class), mock(GenesisBlockFinder.class), cursorService,
                mock(InitService.class), new StoreProperties(), mock(GenesisConfig.class));
    }

    @Test
    void failedStartCanBeRetried() {
        when(cursorService.getStartCursor()).thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> startService.start()).hasMessage("database down");
        assertThat(startService.isStarted()).isFalse();

        // the second try fails for the real reason again, not with "already been started"
        assertThatThrownBy(() -> startService.start()).hasMessage("database down");
    }

    @Test
    void failedRestartLeavesAnErrorNotADeliberateStop() {
        when(cursorService.getStartCursor()).thenThrow(new IllegalStateException("database down"));
        long token = startService.stopForRestart(startService.getStopCount());

        assertThatThrownBy(() -> startService.startAfterRestart(token)).hasMessage("database down");

        assertThat(startService.isStarted()).isFalse();
        verify(blockFetchService).markRestartFailed();
    }

    @Test
    void deliberateStopDuringTheRestartWaitIsKept() {
        long token = startService.stopForRestart(startService.getStopCount());
        startService.stop(); // e.g. the admin API or a balance snapshot

        assertThat(startService.startAfterRestart(token)).isFalse();

        verify(cursorService, never()).getStartCursor();
        verify(blockFetchService, never()).markRestartFailed();
    }

    @Test
    void deliberateStopDuringTheBackoffIsKept() {
        long before = startService.getStopCount();
        startService.stop(); // during the restart's backoff

        assertThat(startService.stopForRestart(before)).isNegative();

        verify(blockFetchService, times(1)).stop();
    }

    @Test
    void startBySomeoneElseDuringTheRestartWaitIsKept() {
        when(cursorService.getStartCursor()).thenThrow(new IllegalStateException("stop here"));
        long token = startService.stopForRestart(startService.getStopCount());
        // pretend another caller started the sync successfully while the restart was waiting
        org.springframework.test.util.ReflectionTestUtils.setField(startService, "alreadyStarted", true);

        assertThat(startService.startAfterRestart(token)).isFalse();

        verify(cursorService, never()).getStartCursor();
        verify(blockFetchService, never()).markRestartFailed();
    }

    @Test
    void stopIsRecordedEvenWhenNothingIsRunning() {
        when(blockFetchService.isRunning()).thenReturn(false);

        startService.stop();

        verify(blockFetchService).stop();
    }

    @Test
    void startClearsTheErrorLeftByAFailedRestart() {
        when(cursorService.getStartCursor()).thenThrow(new IllegalStateException("stop here"));

        assertThatThrownBy(() -> startService.start()).hasMessage("stop here");

        var order = inOrder(blockFetchService);
        order.verify(blockFetchService).clearError();
        order.verify(blockFetchService).reset();
    }
}
