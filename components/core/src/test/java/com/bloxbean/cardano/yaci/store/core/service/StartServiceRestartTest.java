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
    void markRestartFailedClearsTheStopFlag() {
        startService.markRestartFailed();

        assertThat(startService.isStarted()).isFalse();
        verify(blockFetchService).markRestartFailed();
    }
}
